#!/usr/bin/env ruby
# frozen_string_literal: true

require "yaml"

ROOT = File.expand_path("..", __dir__)
INPUT = File.join(ROOT, "docs/openapi.yaml")
OUTPUT = File.join(ROOT, "docs/03-api-reference.md")

doc = YAML.load_file(INPUT)
components = doc.fetch("components")

def resolve(value, root)
  return value unless value.is_a?(Hash) && value["$ref"]

  value.fetch("$ref").delete_prefix("#/").split("/").reduce(root) { |memo, key| memo.fetch(key) }
end

def schema_label(schema, root)
  schema = resolve(schema, root)
  return "未定义" unless schema

  parts = []
  parts << schema["type"] if schema["type"]
  parts << "固定为 `#{schema['const']}`" if schema.key?("const")
  parts << "枚举 `#{schema['enum'].join('|')}`" if schema["enum"]
  parts << "格式 `#{schema['format']}`" if schema["format"]
  parts << "正则 `#{schema['pattern']}`" if schema["pattern"]
  parts << "最小 #{schema['minLength']}" if schema.key?("minLength")
  parts << "最大 #{schema['maxLength']}" if schema.key?("maxLength")
  parts << "最小项 #{schema['minItems']}" if schema.key?("minItems")
  parts << "最大项 #{schema['maxItems']}" if schema.key?("maxItems")
  parts.empty? ? "object" : parts.join("；")
end

def merged_properties(schema, root)
  schema = resolve(schema, root)
  merged = { "properties" => {}, "required" => [] }
  Array(schema["allOf"]).each do |part|
    item = merged_properties(part, root)
    merged["properties"].merge!(item["properties"])
    merged["required"] |= item["required"]
  end
  merged["properties"].merge!(schema.fetch("properties", {}))
  merged["required"] |= schema.fetch("required", [])
  merged
end

def render_schema(schema, root, prefix = "", level = 0, seen = [])
  schema = resolve(schema, root)
  return ["#{'  ' * level}- `#{prefix}`：循环引用"] if seen.include?(schema.object_id)

  result = []
  if schema["oneOf"]
    result << "#{'  ' * level}- `#{prefix}`：以下互斥变体之一"
    schema["oneOf"].each_with_index do |variant, index|
      result << "#{'  ' * (level + 1)}- 变体 #{index + 1}"
      result.concat(render_schema(variant, root, prefix, level + 2, seen + [schema.object_id]))
    end
    return result
  end

  merged = merged_properties(schema, root)
  if merged["properties"].any?
    merged["properties"].each do |name, property|
      path = prefix.empty? ? name : "#{prefix}.#{name}"
      required = merged["required"].include?(name) ? "必填" : "可选"
      result << "#{'  ' * level}- `#{path}`（#{required}）：#{schema_label(property, root)}"
      child = resolve(property, root)
      if child["properties"] || child["allOf"] || child["oneOf"]
        result.concat(render_schema(child, root, path, level + 1, seen + [schema.object_id]))
      elsif child["items"]
        result << "#{'  ' * (level + 1)}- 元素：#{schema_label(child['items'], root)}"
      end
    end
  elsif schema["items"]
    result << "#{'  ' * level}- `#{prefix}`：数组，元素为 #{schema_label(schema['items'], root)}"
  else
    result << "#{'  ' * level}- `#{prefix}`：#{schema_label(schema, root)}"
  end
  result
end

def request_schema(operation, root)
  body = operation["requestBody"]
  return nil unless body

  body = resolve(body, root)
  body.fetch("content", {}).map do |media_type, media|
    [media_type, media.fetch("schema")]
  end
end

def response_schema(response, root)
  response = resolve(response, root)
  response.fetch("content", {}).map do |media_type, media|
    [media_type, media.fetch("schema")]
  end
end

lines = []
lines << "# 狠狠学接口开发手册"
lines << ""
lines << "本文件由 `scripts/generate_api_reference.rb` 根据 [`openapi.yaml`](openapi.yaml) 自动生成。每个端点将路径、认证、参数、请求体、成功响应与错误响应放在同一节；请勿手工编辑。"
lines << ""
lines << "全局响应包装：成功为 `{ code, message, data, traceId }`，失败为 `{ code, message, data: null, traceId }`。业务状态机、错误码语义和幂等规则见 [`03-api-specification.md`](03-api-specification.md)。"
lines << ""

doc.fetch("paths").each do |path, path_item|
  path_parameters = Array(path_item["parameters"])
  path_item.each do |method, operation|
    next unless %w[get post put patch delete].include?(method)

    operation_parameters = path_parameters + Array(operation["parameters"])
    security = operation.key?("security") ? operation["security"] : doc["security"]
    authorization = security == [] ? "公开接口" : "需要 `satoken` 请求头"

    lines << "## #{method.upcase} `#{path}`"
    lines << ""
    lines << "- 操作：`#{operation['operationId']}`，#{operation['summary']}"
    lines << "- 认证：#{authorization}"
    lines << ""

    if operation_parameters.empty?
      lines << "**参数：** 无。"
    else
      lines << "**参数**"
      lines << ""
      lines << "| 名称 | 位置 | 必填 | 规则 |"
      lines << "| --- | --- | --- | --- |"
      operation_parameters.each do |parameter|
        parameter = resolve(parameter, doc)
        lines << "| `#{parameter['name']}` | #{parameter['in']} | #{parameter['required'] ? '是' : '否'} | #{schema_label(parameter.fetch('schema', {}), doc)} |"
      end
    end
    lines << ""

    body_schemas = request_schema(operation, doc)
    if body_schemas.nil? || body_schemas.empty?
      lines << "**请求体：** 无。"
    else
      lines << "**请求体**"
      body_schemas.each do |media_type, schema|
        lines << ""
        lines << "`#{media_type}`"
        lines.concat(render_schema(schema, doc))
      end
    end
    lines << ""

    lines << "**响应**"
    operation.fetch("responses").each do |status, response|
      resolved_response = resolve(response, doc)
      lines << ""
      lines << "- `#{status}`：#{resolved_response['description']}"
      response_schema(response, doc).each do |media_type, schema|
        lines << "  - `#{media_type}`"
        lines.concat(render_schema(schema, doc, "", 2))
      end
    end
    lines << ""
  end
end

File.write(OUTPUT, lines.join("\n"))
puts "Generated #{OUTPUT}"
