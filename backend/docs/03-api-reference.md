# 狠狠学接口开发手册

本文件由 `scripts/generate_api_reference.rb` 根据 [`openapi.yaml`](openapi.yaml) 自动生成。每个端点将路径、认证、参数、请求体、成功响应与错误响应放在同一节；请勿手工编辑。

全局响应包装：成功为 `{ code, message, data, traceId }`，失败为 `{ code, message, data: null, traceId }`。业务状态机、错误码语义和幂等规则见 [`03-api-specification.md`](03-api-specification.md)。

## POST `/auth/register`

- 操作：`register`，Register
- 认证：公开接口

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `username`（必填）：string；最小 8；最大 64
- `email`（必填）：string；格式 `email`
- `emailCode`（必填）：string；正则 `^\\d{6}$`
- `password`（必填）：string；正则 `^(?=.*[A-Za-z])(?=.*\\d).+$`；最小 8；最大 128
- `nickname`（必填）：string；最小 1；最大 64

**响应**

- `201`：User
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.username`（必填）：string
      - `data.email`（必填）：string；格式 `email`
      - `data.emailVerifiedAt`（可选）：string；null；格式 `date-time`
      - `data.nickname`（必填）：string
      - `data.avatarFileId`（可选）：object
        - `data.avatarFileId`：以下互斥变体之一
          - 变体 1
            - `data.avatarFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.avatarFileId`：null
      - `data.permissions`（必填）：array
        - 元素：string
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/auth/login`

- 操作：`login`，Login
- 认证：公开接口

**参数：** 无。

**请求体**

`application/json`
- `account`（必填）：string；最小 1；最大 128
- `password`（必填）：string；最小 8；最大 128

**响应**

- `200`：Login session
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.token`（必填）：string
      - `data.user`（必填）：object
        - `data.user.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
        - `data.user.username`（必填）：string
        - `data.user.email`（必填）：string；格式 `email`
        - `data.user.emailVerifiedAt`（可选）：string；null；格式 `date-time`
        - `data.user.nickname`（必填）：string
        - `data.user.avatarFileId`（可选）：object
          - `data.user.avatarFileId`：以下互斥变体之一
            - 变体 1
              - `data.user.avatarFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
            - 变体 2
              - `data.user.avatarFileId`：null
        - `data.user.permissions`（必填）：array
          - 元素：string
        - `data.user.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

- `401`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

- `429`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

- `503`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/auth/email-codes`

- 操作：`sendEmailCode`，Send email code
- 认证：公开接口

**参数：** 无。

**请求体**

`application/json`
- `email`（必填）：string；格式 `email`
- `purpose`（必填）：string；枚举 `REGISTER|PASSWORD_RESET`

**响应**

- `202`：

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

- `429`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/auth/password-resets`

- 操作：`resetPassword`，Reset password
- 认证：公开接口

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `email`（必填）：string；格式 `email`
- `emailCode`（必填）：string；正则 `^\\d{6}$`
- `newPassword`（必填）：string；最小 8；最大 128

**响应**

- `204`：Completed without a response body

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/auth/password-changes`

- 操作：`changePassword`，Change password
- 认证：需要 `satoken` 请求头

**参数：** 无。

**请求体**

`application/json`
- `oldPassword`（必填）：string；最小 8；最大 128
- `newPassword`（必填）：string；最小 8；最大 128

**响应**

- `204`：Completed without a response body

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/auth/logout`

- 操作：`logout`，Logout
- 认证：需要 `satoken` 请求头

**参数：** 无。

**请求体：** 无。

**响应**

- `204`：Completed without a response body

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/auth/refresh`

- 操作：`refreshSession`，Refresh session
- 认证：需要 `satoken` 请求头

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：Login session
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.token`（必填）：string
      - `data.user`（必填）：object
        - `data.user.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
        - `data.user.username`（必填）：string
        - `data.user.email`（必填）：string；格式 `email`
        - `data.user.emailVerifiedAt`（可选）：string；null；格式 `date-time`
        - `data.user.nickname`（必填）：string
        - `data.user.avatarFileId`（可选）：object
          - `data.user.avatarFileId`：以下互斥变体之一
            - 变体 1
              - `data.user.avatarFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
            - 变体 2
              - `data.user.avatarFileId`：null
        - `data.user.permissions`（必填）：array
          - 元素：string
        - `data.user.version`（必填）：integer
    - `traceId`（必填）：string

- `401`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/users/me`

- 操作：`getCurrentUser`，Get current user
- 认证：需要 `satoken` 请求头

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：User
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.username`（必填）：string
      - `data.email`（必填）：string；格式 `email`
      - `data.emailVerifiedAt`（可选）：string；null；格式 `date-time`
      - `data.nickname`（必填）：string
      - `data.avatarFileId`（可选）：object
        - `data.avatarFileId`：以下互斥变体之一
          - 变体 1
            - `data.avatarFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.avatarFileId`：null
      - `data.permissions`（必填）：array
        - 元素：string
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `401`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## PATCH `/users/me`

- 操作：`patchCurrentUser`，Patch current user
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |

**请求体**

`application/json`
- `nickname`（可选）：string；最小 1；最大 64
- `avatarFileId`（可选）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `preferences`（可选）：object
  - `preferences.defaultPracticeMode`（可选）：string；枚举 `MEMORIZE|EXAM`

**响应**

- `200`：User
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.username`（必填）：string
      - `data.email`（必填）：string；格式 `email`
      - `data.emailVerifiedAt`（可选）：string；null；格式 `date-time`
      - `data.nickname`（必填）：string
      - `data.avatarFileId`（可选）：object
        - `data.avatarFileId`：以下互斥变体之一
          - 变体 1
            - `data.avatarFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.avatarFileId`：null
      - `data.permissions`（必填）：array
        - 元素：string
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/ai-credentials`

- 操作：`listAiCredentials`，List AI credentials
- 认证：需要 `satoken` 请求头

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：Credentials
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：array
      - 元素：object
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/ai-credentials`

- 操作：`createAiCredential`，Create AI credential
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `providerCode`（必填）：string；枚举 `DEEPSEEK|DASHSCOPE`
- `model`（必填）：string；最小 1；最大 128
- `apiKey`（必填）：string；最小 1；最大 512

**响应**

- `201`：Credential
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.providerCode`（必填）：string；枚举 `DEEPSEEK|DASHSCOPE`
      - `data.model`（必填）：string
      - `data.keyFingerprintSuffix`（必填）：string
      - `data.status`（必填）：string；枚举 `VERIFYING|VERIFIED|FAILED|REVOKED`
      - `data.verifiedAt`（可选）：string；null；格式 `date-time`
      - `data.lastUsedAt`（可选）：string；null；格式 `date-time`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `422`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## PATCH `/ai-credentials/{credentialId}`

- 操作：`patchAiCredential`，Patch AI credential
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `credentialId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `providerCode`（必填）：string；枚举 `DEEPSEEK|DASHSCOPE`
- `model`（必填）：string；最小 1；最大 128
- `apiKey`（必填）：string；最小 1；最大 512

**响应**

- `200`：Credential
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.providerCode`（必填）：string；枚举 `DEEPSEEK|DASHSCOPE`
      - `data.model`（必填）：string
      - `data.keyFingerprintSuffix`（必填）：string
      - `data.status`（必填）：string；枚举 `VERIFYING|VERIFIED|FAILED|REVOKED`
      - `data.verifiedAt`（可选）：string；null；格式 `date-time`
      - `data.lastUsedAt`（可选）：string；null；格式 `date-time`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## DELETE `/ai-credentials/{credentialId}`

- 操作：`revokeAiCredential`，Revoke AI credential
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `credentialId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `204`：Completed without a response body

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/articles`

- 操作：`listArticles`，List articles
- 认证：公开接口

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | query | 否 | integer |
| `pageSize` | query | 否 | integer |
| `keyword` | query | 否 | string；最大 128 |
| `categorySlug` | query | 否 | string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128 |
| `tagSlug` | query | 否 | string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128 |
| `sort` | query | 否 | string；枚举 `RELEVANCE|PUBLISHED_AT` |

**请求体：** 无。

**响应**

- `200`：Article page
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.items`（必填）：array
        - 元素：object
      - `data.page`（必填）：integer
      - `data.pageSize`（必填）：integer
      - `data.total`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/articles/{slug}`

- 操作：`getArticle`，Get article
- 认证：公开接口

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `slug` | path | 是 | string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128 |

**请求体：** 无。

**响应**

- `200`：Article
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.title`（必填）：string
      - `data.summary`（必填）：string
      - `data.coverFileId`（可选）：object
        - `data.coverFileId`：以下互斥变体之一
          - 变体 1
            - `data.coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.coverFileId`：null
      - `data.categories`（可选）：array
        - 元素：object
      - `data.tags`（可选）：array
        - 元素：object
      - `data.contentMd`（必填）：string
      - `data.toc`（必填）：array
        - 元素：object
      - `data.currentRevisionNo`（必填）：integer
      - `data.publishedAt`（可选）：string；格式 `date-time`
      - `data.readingMinutes`（可选）：integer
      - `data.favoriteCount`（可选）：integer
      - `data.isFavorited`（可选）：boolean
    - `traceId`（必填）：string

- `404`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/pages/about`

- 操作：`getAboutPage`，Get about page
- 认证：公开接口

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：Article
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.title`（必填）：string
      - `data.summary`（必填）：string
      - `data.coverFileId`（可选）：object
        - `data.coverFileId`：以下互斥变体之一
          - 变体 1
            - `data.coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.coverFileId`：null
      - `data.categories`（可选）：array
        - 元素：object
      - `data.tags`（可选）：array
        - 元素：object
      - `data.contentMd`（必填）：string
      - `data.toc`（必填）：array
        - 元素：object
      - `data.currentRevisionNo`（必填）：integer
      - `data.publishedAt`（可选）：string；格式 `date-time`
      - `data.readingMinutes`（可选）：integer
      - `data.favoriteCount`（可选）：integer
      - `data.isFavorited`（可选）：boolean
    - `traceId`（必填）：string

- `404`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/categories`

- 操作：`listCategories`，List categories
- 认证：公开接口

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：Categories
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：array
      - 元素：object
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/tags`

- 操作：`listTags`，List tags
- 认证：公开接口

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：Tags
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：array
      - 元素：object
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/archives`

- 操作：`listArchives`，List archives
- 认证：公开接口

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：Archives
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：array
      - 元素：object
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/articles/{articleId}/favorite`

- 操作：`favoriteArticle`，Favorite article
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `articleId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `204`：Completed without a response body

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## DELETE `/articles/{articleId}/favorite`

- 操作：`unfavoriteArticle`，Unfavorite article
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `articleId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `204`：Completed without a response body

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/admin/articles`

- 操作：`createArticleDraft`，Create article draft
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `articleType`（必填）：string；枚举 `ARTICLE|PAGE`
- `slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
- `title`（必填）：string；最小 1；最大 128
- `summary`（必填）：string；最大 500
- `coverFileId`（可选）：object
  - `coverFileId`：以下互斥变体之一
    - 变体 1
      - `coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
    - 变体 2
      - `coverFileId`：null
- `contentMd`（必填）：string；最大 102400
- `categoryIds`（必填）：array；最大项 5
  - 元素：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `tagIds`（必填）：array；最大项 20
  - 元素：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `changeNote`（可选）：string；最大 500

**响应**

- `201`：Article for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.title`（必填）：string
      - `data.summary`（必填）：string
      - `data.coverFileId`（可选）：object
        - `data.coverFileId`：以下互斥变体之一
          - 变体 1
            - `data.coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.coverFileId`：null
      - `data.categories`（可选）：array
        - 元素：object
      - `data.tags`（可选）：array
        - 元素：object
      - `data.contentMd`（必填）：string
      - `data.toc`（必填）：array
        - 元素：object
      - `data.currentRevisionNo`（必填）：integer
      - `data.publishedAt`（可选）：string；格式 `date-time`
      - `data.readingMinutes`（可选）：integer
      - `data.favoriteCount`（可选）：integer
      - `data.isFavorited`（可选）：boolean
      - `data.status`（必填）：string；枚举 `DRAFT|SCHEDULED|PUBLISHED|OFFLINE|ARCHIVED`
      - `data.scheduledAt`（可选）：string；null；格式 `date-time`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## PATCH `/admin/articles/{articleId}`

- 操作：`patchArticle`，Patch article
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `articleId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |

**请求体**

`application/json`
- `articleType`（必填）：string；枚举 `ARTICLE|PAGE`
- `slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
- `title`（必填）：string；最小 1；最大 128
- `summary`（必填）：string；最大 500
- `coverFileId`（可选）：object
  - `coverFileId`：以下互斥变体之一
    - 变体 1
      - `coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
    - 变体 2
      - `coverFileId`：null
- `contentMd`（必填）：string；最大 102400
- `categoryIds`（必填）：array；最大项 5
  - 元素：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `tagIds`（必填）：array；最大项 20
  - 元素：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `changeNote`（可选）：string；最大 500

**响应**

- `200`：Article for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.title`（必填）：string
      - `data.summary`（必填）：string
      - `data.coverFileId`（可选）：object
        - `data.coverFileId`：以下互斥变体之一
          - 变体 1
            - `data.coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.coverFileId`：null
      - `data.categories`（可选）：array
        - 元素：object
      - `data.tags`（可选）：array
        - 元素：object
      - `data.contentMd`（必填）：string
      - `data.toc`（必填）：array
        - 元素：object
      - `data.currentRevisionNo`（必填）：integer
      - `data.publishedAt`（可选）：string；格式 `date-time`
      - `data.readingMinutes`（可选）：integer
      - `data.favoriteCount`（可选）：integer
      - `data.isFavorited`（可选）：boolean
      - `data.status`（必填）：string；枚举 `DRAFT|SCHEDULED|PUBLISHED|OFFLINE|ARCHIVED`
      - `data.scheduledAt`（可选）：string；null；格式 `date-time`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/admin/articles/{articleId}/publish`

- 操作：`publishArticle`，Publish article
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `articleId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `scheduledAt`（可选）：string；格式 `date-time`

**响应**

- `200`：Article for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.title`（必填）：string
      - `data.summary`（必填）：string
      - `data.coverFileId`（可选）：object
        - `data.coverFileId`：以下互斥变体之一
          - 变体 1
            - `data.coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.coverFileId`：null
      - `data.categories`（可选）：array
        - 元素：object
      - `data.tags`（可选）：array
        - 元素：object
      - `data.contentMd`（必填）：string
      - `data.toc`（必填）：array
        - 元素：object
      - `data.currentRevisionNo`（必填）：integer
      - `data.publishedAt`（可选）：string；格式 `date-time`
      - `data.readingMinutes`（可选）：integer
      - `data.favoriteCount`（可选）：integer
      - `data.isFavorited`（可选）：boolean
      - `data.status`（必填）：string；枚举 `DRAFT|SCHEDULED|PUBLISHED|OFFLINE|ARCHIVED`
      - `data.scheduledAt`（可选）：string；null；格式 `date-time`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/admin/articles/{articleId}/offline`

- 操作：`offlineArticle`，Offline article
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `articleId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `200`：Article for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.title`（必填）：string
      - `data.summary`（必填）：string
      - `data.coverFileId`（可选）：object
        - `data.coverFileId`：以下互斥变体之一
          - 变体 1
            - `data.coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.coverFileId`：null
      - `data.categories`（可选）：array
        - 元素：object
      - `data.tags`（可选）：array
        - 元素：object
      - `data.contentMd`（必填）：string
      - `data.toc`（必填）：array
        - 元素：object
      - `data.currentRevisionNo`（必填）：integer
      - `data.publishedAt`（可选）：string；格式 `date-time`
      - `data.readingMinutes`（可选）：integer
      - `data.favoriteCount`（可选）：integer
      - `data.isFavorited`（可选）：boolean
      - `data.status`（必填）：string；枚举 `DRAFT|SCHEDULED|PUBLISHED|OFFLINE|ARCHIVED`
      - `data.scheduledAt`（可选）：string；null；格式 `date-time`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/admin/articles/{articleId}/revisions`

- 操作：`listArticleRevisions`，List article revisions
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `articleId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Revisions
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：array
      - 元素：object
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/admin/articles/{articleId}/revisions/{revisionId}/restore`

- 操作：`restoreArticleRevision`，Restore article revision
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `articleId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `revisionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `200`：Article for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.title`（必填）：string
      - `data.summary`（必填）：string
      - `data.coverFileId`（可选）：object
        - `data.coverFileId`：以下互斥变体之一
          - 变体 1
            - `data.coverFileId`：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - 变体 2
            - `data.coverFileId`：null
      - `data.categories`（可选）：array
        - 元素：object
      - `data.tags`（可选）：array
        - 元素：object
      - `data.contentMd`（必填）：string
      - `data.toc`（必填）：array
        - 元素：object
      - `data.currentRevisionNo`（必填）：integer
      - `data.publishedAt`（可选）：string；格式 `date-time`
      - `data.readingMinutes`（可选）：integer
      - `data.favoriteCount`（可选）：integer
      - `data.isFavorited`（可选）：boolean
      - `data.status`（必填）：string；枚举 `DRAFT|SCHEDULED|PUBLISHED|OFFLINE|ARCHIVED`
      - `data.scheduledAt`（可选）：string；null；格式 `date-time`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/admin/categories`

- 操作：`createCategory`，Create category
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `name`（必填）：string；最小 1；最大 128
- `slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
- `description`（可选）：string；最大 500
- `sortOrder`（必填）：integer
- `status`（必填）：string；枚举 `ACTIVE|ARCHIVED`

**响应**

- `201`：Category for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.name`（必填）：string
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.description`（可选）：string
      - `data.sortOrder`（必填）：integer
      - `data.status`（必填）：string；枚举 `ACTIVE|ARCHIVED`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## PATCH `/admin/categories/{categoryId}`

- 操作：`patchCategory`，Patch category
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `categoryId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |

**请求体**

`application/json`
- `name`（必填）：string；最小 1；最大 128
- `slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
- `description`（可选）：string；最大 500
- `sortOrder`（必填）：integer
- `status`（必填）：string；枚举 `ACTIVE|ARCHIVED`

**响应**

- `200`：Category for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.name`（必填）：string
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.description`（可选）：string
      - `data.sortOrder`（必填）：integer
      - `data.status`（必填）：string；枚举 `ACTIVE|ARCHIVED`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/admin/categories/{categoryId}/archive`

- 操作：`archiveCategory`，Archive category
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `categoryId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `200`：Category for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.name`（必填）：string
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.description`（可选）：string
      - `data.sortOrder`（必填）：integer
      - `data.status`（必填）：string；枚举 `ACTIVE|ARCHIVED`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/admin/tags`

- 操作：`createTag`，Create tag
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `name`（必填）：string；最小 1；最大 128
- `slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
- `status`（必填）：string；枚举 `ACTIVE|ARCHIVED`

**响应**

- `201`：Tag for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.name`（必填）：string
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.status`（必填）：string；枚举 `ACTIVE|ARCHIVED`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## PATCH `/admin/tags/{tagId}`

- 操作：`patchTag`，Patch tag
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `tagId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |

**请求体**

`application/json`
- `name`（必填）：string；最小 1；最大 128
- `slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
- `status`（必填）：string；枚举 `ACTIVE|ARCHIVED`

**响应**

- `200`：Tag for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.name`（必填）：string
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.status`（必填）：string；枚举 `ACTIVE|ARCHIVED`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/admin/tags/{tagId}/archive`

- 操作：`archiveTag`，Archive tag
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `tagId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `200`：Tag for administration
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.name`（必填）：string
      - `data.slug`（必填）：string；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；最大 128
      - `data.status`（必填）：string；枚举 `ACTIVE|ARCHIVED`
      - `data.version`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/files/upload-credentials`

- 操作：`createUploadCredential`，Create upload credential
- 认证：需要 `satoken` 请求头

**参数：** 无。

**请求体**

`application/json`
- `purpose`（必填）：string；枚举 `ARTICLE_CONTENT_ASSET|QUESTION_MINERU_ARCHIVE`
- `fileName`（必填）：string；最小 1；最大 255
- `contentType`（必填）：string；最大 128
- `size`（必填）：integer
- `sha256`（必填）：string；正则 `^[a-fA-F0-9]{64}$`

**响应**

- `201`：Upload credential
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.fileId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.uploadUrl`（必填）：string；格式 `uri`
      - `data.requiredHeaders`（必填）：object
      - `data.expiresAt`（必填）：string；格式 `date-time`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/files/{fileId}/complete`

- 操作：`completeUpload`，Complete upload
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `fileId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `200`：File object
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.originalName`（必填）：string
      - `data.contentType`（必填）：string
      - `data.size`（必填）：integer
      - `data.sha256`（必填）：string
      - `data.purpose`（必填）：string
      - `data.status`（必填）：string
    - `traceId`（必填）：string

- `422`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/files/{fileId}/download-url`

- 操作：`getDownloadUrl`，Get download url
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `fileId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Download URL
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.url`（必填）：string；格式 `uri`
      - `data.expiresAt`（必填）：string；格式 `date-time`
    - `traceId`（必填）：string

- `404`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/tasks/{taskId}`

- 操作：`getTask`，Get task
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `taskId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Task
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.taskId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.taskType`（必填）：string；枚举 `LEARNING_TREE_GENERATE|LEARNING_FOLLOW_UP|QUESTION_IMPORT`
      - `data.status`（必填）：string；枚举 `PENDING|RUNNING|SUCCEEDED|FAILED|CANCELLED`
      - `data.progress`（必填）：integer
      - `data.resourceType`（必填）：string
      - `data.resourceId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.errorCode`（可选）：string；null
      - `data.errorMessage`（可选）：string；null
      - `data.startedAt`（可选）：string；null；格式 `date-time`
      - `data.finishedAt`（可选）：string；null；格式 `date-time`
    - `traceId`（必填）：string

- `404`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/learning/trees`

- 操作：`listTrees`，List trees
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | query | 否 | integer |
| `pageSize` | query | 否 | integer |

**请求体：** 无。

**响应**

- `200`：Tree page
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.items`（必填）：array
        - 元素：object
      - `data.page`（必填）：integer
      - `data.pageSize`（必填）：integer
      - `data.total`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/learning/trees`

- 操作：`createTree`，Create tree
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `title`（必填）：string；最小 1；最大 128
- `originalQuestion`（必填）：string；最小 1；最大 8192
- `credentialId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `language`（必填）：string；枚举 `zh-CN|en-US`

**响应**

- `202`：Task accepted
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.taskId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.status`（必填）：固定为 `PENDING`
      - `data.resourceType`（必填）：string
      - `data.resourceId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/learning/trees/{treeId}`

- 操作：`getTree`，Get tree
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `treeId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Tree
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.treeId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.rootNodeKey`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.nodes`（必填）：array
        - 元素：object
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## PATCH `/learning/trees/{treeId}/credential`

- 操作：`patchTreeCredential`，Patch tree credential
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `treeId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |

**请求体**

`application/json`
- `credentialId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`

**响应**

- `200`：Tree
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.treeId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.rootNodeKey`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.nodes`（必填）：array
        - 元素：object
    - `traceId`（必填）：string

- `409`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/learning/trees/{treeId}/nodes/{nodeKey}/content`

- 操作：`getTreeNodeContent`，Get tree node content
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `treeId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `nodeKey` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Node content
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.treeId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.nodeKey`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.title`（必填）：string
      - `data.path`（必填）：array
        - 元素：object
      - `data.isLeaf`（必填）：boolean
      - `data.followUpAllowed`（必填）：boolean
      - `data.displayScope`（必填）：string；枚举 `DIRECT|SUBTREE`
      - `data.displayMd`（必填）：string
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/learning/trees/{treeId}/follow-ups`

- 操作：`createFollowUp`，Create follow up
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `treeId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `nodeKey`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `question`（必填）：string；最小 1；最大 8192

**响应**

- `202`：Follow-up accepted
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.conversationId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.task`（必填）：object
        - `data.task.taskId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
        - `data.task.status`（必填）：固定为 `PENDING`
        - `data.task.resourceType`（必填）：string
        - `data.task.resourceId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
    - `traceId`（必填）：string

- `422`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/learning/trees/{treeId}/conversations`

- 操作：`listNodeConversations`，List node conversations
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `treeId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `nodeKey` | query | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Conversations
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：array
      - 元素：object
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/learning/conversations/{conversationId}/messages`

- 操作：`listConversationMessages`，List conversation messages
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `conversationId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `page` | query | 否 | integer |
| `pageSize` | query | 否 | integer |

**请求体：** 无。

**响应**

- `200`：Message page
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.items`（必填）：array
        - 元素：object
      - `data.page`（必填）：integer
      - `data.pageSize`（必填）：integer
      - `data.total`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/question-libraries`

- 操作：`listQuestionLibraries`，List question libraries
- 认证：需要 `satoken` 请求头

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：Libraries
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：array
      - 元素：object
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/question-libraries`

- 操作：`createQuestionLibrary`，Create question library
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `name`（必填）：string；最小 1；最大 128
- `description`（可选）：string；最大 500

**响应**

- `201`：Library
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.name`（必填）：string
      - `data.description`（可选）：string
      - `data.libraryType`（必填）：string；枚举 `NORMAL|WRONG_QUESTION`
      - `data.status`（必填）：string
      - `data.questionCount`（必填）：integer
      - `data.version`（必填）：integer
      - `data.updatedAt`（必填）：string；格式 `date-time`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## PATCH `/question-libraries/{libraryId}`

- 操作：`patchQuestionLibrary`，Patch question library
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `libraryId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |

**请求体**

`application/json`
- `name`（可选）：string；最小 1；最大 128
- `description`（可选）：string；最大 500

**响应**

- `200`：Library
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.name`（必填）：string
      - `data.description`（可选）：string
      - `data.libraryType`（必填）：string；枚举 `NORMAL|WRONG_QUESTION`
      - `data.status`（必填）：string
      - `data.questionCount`（必填）：integer
      - `data.version`（必填）：integer
      - `data.updatedAt`（必填）：string；格式 `date-time`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/question-libraries/{libraryId}/imports`

- 操作：`createQuestionImport`，Create question import
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `libraryId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `fileId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `credentialId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`

**响应**

- `202`：Task accepted
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.taskId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.status`（必填）：固定为 `PENDING`
      - `data.resourceType`（必填）：string
      - `data.resourceId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/question-imports/{importId}`

- 操作：`getQuestionImport`，Get question import
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `importId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Import
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.libraryId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.taskId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.status`（必填）：string
      - `data.totalCount`（必填）：integer
      - `data.successCount`（必填）：integer
      - `data.failedCount`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/question-libraries/{libraryId}/questions`

- 操作：`listLibraryQuestions`，List library questions
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `libraryId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `page` | query | 否 | integer |
| `pageSize` | query | 否 | integer |

**请求体：** 无。

**响应**

- `200`：Question page
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.items`（必填）：array
        - 元素：object
      - `data.page`（必填）：integer
      - `data.pageSize`（必填）：integer
      - `data.total`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/question-libraries/{libraryId}/questions`

- 操作：`createQuestion`，Create question
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `libraryId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- ``：以下互斥变体之一
  - 变体 1
    - `type`（必填）：固定为 `SINGLE_CHOICE`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `options`（必填）：array；最小项 2
      - 元素：object
    - `answer`（必填）：object
      - `answer.keys`（必填）：array；最小项 1；最大项 1
        - 元素：string；正则 `^[A-Z]{1,3}$`
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64
  - 变体 2
    - `type`（必填）：固定为 `MULTIPLE_CHOICE`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `options`（必填）：array；最小项 2
      - 元素：object
    - `answer`（必填）：object
      - `answer.keys`（必填）：array；最小项 1
        - 元素：string；正则 `^[A-Z]{1,3}$`
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64
  - 变体 3
    - `type`（必填）：固定为 `JUDGE`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `answer`（必填）：object
      - `answer.value`（必填）：boolean
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64
  - 变体 4
    - `type`（必填）：固定为 `FILL_BLANK`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `answer`（必填）：object
      - `answer.answers`（必填）：array；最小项 1
        - 元素：string；最小 1；最大 1024
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64
  - 变体 5
    - `type`（必填）：固定为 `ESSAY`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `answer`（必填）：object
      - `answer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64

**响应**

- `201`：Question
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.type`（必填）：string
      - `data.stemMd`（必填）：string
      - `data.options`（可选）：array
        - 元素：object
      - `data.analysisMd`（必填）：string
      - `data.difficulty`（必填）：integer
      - `data.knowledgeTags`（必填）：array
        - 元素：string
      - `data.status`（必填）：string
      - `data.version`（必填）：integer
      - `data.answer`（必填）：object
        - `data.answer`：以下互斥变体之一
          - 变体 1
            - `data.answer.keys`（必填）：array；最小项 1
              - 元素：string；正则 `^[A-Z]{1,3}$`
          - 变体 2
            - `data.answer.value`（必填）：boolean
          - 变体 3
            - `data.answer.answers`（必填）：array；最小项 1
              - 元素：string；最小 1；最大 1024
          - 变体 4
            - `data.answer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## PATCH `/questions/{questionId}`

- 操作：`patchQuestion`，Patch question
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `questionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |

**请求体**

`application/json`
- ``：以下互斥变体之一
  - 变体 1
    - `type`（必填）：固定为 `SINGLE_CHOICE`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `options`（必填）：array；最小项 2
      - 元素：object
    - `answer`（必填）：object
      - `answer.keys`（必填）：array；最小项 1；最大项 1
        - 元素：string；正则 `^[A-Z]{1,3}$`
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64
  - 变体 2
    - `type`（必填）：固定为 `MULTIPLE_CHOICE`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `options`（必填）：array；最小项 2
      - 元素：object
    - `answer`（必填）：object
      - `answer.keys`（必填）：array；最小项 1
        - 元素：string；正则 `^[A-Z]{1,3}$`
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64
  - 变体 3
    - `type`（必填）：固定为 `JUDGE`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `answer`（必填）：object
      - `answer.value`（必填）：boolean
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64
  - 变体 4
    - `type`（必填）：固定为 `FILL_BLANK`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `answer`（必填）：object
      - `answer.answers`（必填）：array；最小项 1
        - 元素：string；最小 1；最大 1024
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64
  - 变体 5
    - `type`（必填）：固定为 `ESSAY`
    - `stemMd`（必填）：string；最小 1；最大 102400
    - `answer`（必填）：object
      - `answer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`
    - `analysisMd`（必填）：string；最大 102400
    - `difficulty`（必填）：integer
    - `knowledgeTags`（必填）：array；最大项 20
      - 元素：string；最小 1；最大 64

**响应**

- `200`：Question
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.type`（必填）：string
      - `data.stemMd`（必填）：string
      - `data.options`（可选）：array
        - 元素：object
      - `data.analysisMd`（必填）：string
      - `data.difficulty`（必填）：integer
      - `data.knowledgeTags`（必填）：array
        - 元素：string
      - `data.status`（必填）：string
      - `data.version`（必填）：integer
      - `data.answer`（必填）：object
        - `data.answer`：以下互斥变体之一
          - 变体 1
            - `data.answer.keys`（必填）：array；最小项 1
              - 元素：string；正则 `^[A-Z]{1,3}$`
          - 变体 2
            - `data.answer.value`（必填）：boolean
          - 变体 3
            - `data.answer.answers`（必填）：array；最小项 1
              - 元素：string；最小 1；最大 1024
          - 变体 4
            - `data.answer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/questions/{questionId}/publish`

- 操作：`publishQuestion`，Publish question
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `questionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `200`：Question
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.type`（必填）：string
      - `data.stemMd`（必填）：string
      - `data.options`（可选）：array
        - 元素：object
      - `data.analysisMd`（必填）：string
      - `data.difficulty`（必填）：integer
      - `data.knowledgeTags`（必填）：array
        - 元素：string
      - `data.status`（必填）：string
      - `data.version`（必填）：integer
      - `data.answer`（必填）：object
        - `data.answer`：以下互斥变体之一
          - 变体 1
            - `data.answer.keys`（必填）：array；最小项 1
              - 元素：string；正则 `^[A-Z]{1,3}$`
          - 变体 2
            - `data.answer.value`（必填）：boolean
          - 变体 3
            - `data.answer.answers`（必填）：array；最小项 1
              - 元素：string；最小 1；最大 1024
          - 变体 4
            - `data.answer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/questions/{questionId}/archive`

- 操作：`archiveQuestion`，Archive question
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `questionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `If-Match` | header | 是 | string；正则 `^"[1-9][0-9]*"$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `200`：Question
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.type`（必填）：string
      - `data.stemMd`（必填）：string
      - `data.options`（可选）：array
        - 元素：object
      - `data.analysisMd`（必填）：string
      - `data.difficulty`（必填）：integer
      - `data.knowledgeTags`（必填）：array
        - 元素：string
      - `data.status`（必填）：string
      - `data.version`（必填）：integer
      - `data.answer`（必填）：object
        - `data.answer`：以下互斥变体之一
          - 变体 1
            - `data.answer.keys`（必填）：array；最小项 1
              - 元素：string；正则 `^[A-Z]{1,3}$`
          - 变体 2
            - `data.answer.value`（必填）：boolean
          - 变体 3
            - `data.answer.answers`（必填）：array；最小项 1
              - 元素：string；最小 1；最大 1024
          - 变体 4
            - `data.answer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## DELETE `/question-libraries/{libraryId}/questions/{questionId}`

- 操作：`removeQuestionFromLibrary`，Remove question from library
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `libraryId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `questionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `204`：Completed without a response body

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/practice-sessions`

- 操作：`listPracticeSessions`，List practice sessions
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `page` | query | 否 | integer |
| `pageSize` | query | 否 | integer |

**请求体：** 无。

**响应**

- `200`：Practice page
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.items`（必填）：array
        - 元素：object
      - `data.page`（必填）：integer
      - `data.pageSize`（必填）：integer
      - `data.total`（必填）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/practice-sessions`

- 操作：`createPracticeSession`，Create practice session
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `scope`（必填）：string；枚举 `SELECTED_LIBRARIES|ALL_MY_LIBRARIES`
- `libraryIds`（必填）：array；最大项 100
  - 元素：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `mode`（必填）：string；枚举 `MEMORIZE|EXAM`
- `questionTypes`（必填）：array；最小项 1
  - 元素：string；枚举 `SINGLE_CHOICE|MULTIPLE_CHOICE|JUDGE|FILL_BLANK|ESSAY`
- `questionCount`（必填）：integer
- `order`（必填）：string；枚举 `RANDOM|SEQUENTIAL`

**响应**

- `201`：Practice session
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.mode`（必填）：string；枚举 `MEMORIZE|EXAM`
      - `data.status`（必填）：string
      - `data.questionCount`（必填）：integer
      - `data.answeredCount`（必填）：integer
      - `data.correctCount`（必填）：integer
      - `data.startedAt`（必填）：string；格式 `date-time`
      - `data.completedAt`（可选）：string；null；格式 `date-time`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/practice-sessions/{sessionId}`

- 操作：`getPracticeSession`，Get practice session
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `sessionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Practice session
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.mode`（必填）：string；枚举 `MEMORIZE|EXAM`
      - `data.status`（必填）：string
      - `data.questionCount`（必填）：integer
      - `data.answeredCount`（必填）：integer
      - `data.correctCount`（必填）：integer
      - `data.startedAt`（必填）：string；格式 `date-time`
      - `data.completedAt`（可选）：string；null；格式 `date-time`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/practice-sessions/{sessionId}/next`

- 操作：`getNextPracticeQuestion`，Get next practice question
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `sessionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |

**请求体：** 无。

**响应**

- `200`：Practice question
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data`：以下互斥变体之一
        - 变体 1
          - `data.mode`（必填）：固定为 `EXAM`
          - `data.questionSnapshotId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - `data.sequenceNo`（必填）：integer
          - `data.question`（必填）：object
            - `data.question.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
            - `data.question.type`（必填）：string
            - `data.question.stemMd`（必填）：string
            - `data.question.options`（可选）：array
              - 元素：object
        - 变体 2
          - `data.mode`（必填）：固定为 `MEMORIZE`
          - `data.questionSnapshotId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
          - `data.sequenceNo`（必填）：integer
          - `data.question`（必填）：object
            - `data.question.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
            - `data.question.type`（必填）：string
            - `data.question.stemMd`（必填）：string
            - `data.question.options`（可选）：array
              - 元素：object
          - `data.correctAnswer`（必填）：object
            - `data.correctAnswer`：以下互斥变体之一
              - 变体 1
                - `data.correctAnswer.keys`（必填）：array；最小项 1
                  - 元素：string；正则 `^[A-Z]{1,3}$`
              - 变体 2
                - `data.correctAnswer.value`（必填）：boolean
              - 变体 3
                - `data.correctAnswer.answers`（必填）：array；最小项 1
                  - 元素：string；最小 1；最大 1024
              - 变体 4
                - `data.correctAnswer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`
          - `data.analysisMd`（必填）：string
    - `traceId`（必填）：string

- `404`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/practice-sessions/{sessionId}/answers`

- 操作：`submitPracticeAnswer`，Submit practice answer
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `sessionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体**

`application/json`
- `questionSnapshotId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
- `answer`（必填）：object
  - `answer`：以下互斥变体之一
    - 变体 1
      - `answer.keys`（必填）：array；最小项 1
        - 元素：string；正则 `^[A-Z]{1,3}$`
    - 变体 2
      - `answer.value`（必填）：boolean
    - 变体 3
      - `answer.answers`（必填）：array；最小项 1
        - 元素：string；最小 1；最大 1024
    - 变体 4
      - `answer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`

**响应**

- `200`：Practice answer
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.result`（必填）：string；枚举 `CORRECT|INCORRECT|DONT_KNOW|NOT_MASTERED`
      - `data.correctAnswer`（必填）：object
        - `data.correctAnswer`：以下互斥变体之一
          - 变体 1
            - `data.correctAnswer.keys`（必填）：array；最小项 1
              - 元素：string；正则 `^[A-Z]{1,3}$`
          - 变体 2
            - `data.correctAnswer.value`（必填）：boolean
          - 变体 3
            - `data.correctAnswer.answers`（必填）：array；最小项 1
              - 元素：string；最小 1；最大 1024
          - 变体 4
            - `data.correctAnswer.selfAssessment`（必填）：string；枚举 `KNOW|DONT_KNOW`
      - `data.analysisMd`（必填）：string
      - `data.wrongLibrary`（必填）：object
        - `data.wrongLibrary.libraryId`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
        - `data.wrongLibrary.containsQuestion`（必填）：boolean
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## POST `/practice-sessions/{sessionId}/finish`

- 操作：`finishPracticeSession`，Finish practice session
- 认证：需要 `satoken` 请求头

**参数**

| 名称 | 位置 | 必填 | 规则 |
| --- | --- | --- | --- |
| `sessionId` | path | 是 | string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$` |
| `Idempotency-Key` | header | 是 | string；格式 `uuid` |

**请求体：** 无。

**响应**

- `200`：Practice session
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.id`（必填）：string；正则 `^(?:[0-9]{26}|[0-9A-HJKMNP-TV-Z]{26})$`
      - `data.mode`（必填）：string；枚举 `MEMORIZE|EXAM`
      - `data.status`（必填）：string
      - `data.questionCount`（必填）：integer
      - `data.answeredCount`（必填）：integer
      - `data.correctCount`（必填）：integer
      - `data.startedAt`（必填）：string；格式 `date-time`
      - `data.completedAt`（可选）：string；null；格式 `date-time`
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string

## GET `/practice-statistics`

- 操作：`getPracticeStatistics`，Get practice statistics
- 认证：需要 `satoken` 请求头

**参数：** 无。

**请求体：** 无。

**响应**

- `200`：Statistics
  - `application/json`
    - `code`（必填）：固定为 `0`
    - `message`（必填）：固定为 `OK`
    - `data`（必填）：object
      - `data.masteryLevel`（可选）：object
      - `data.correctRate`（可选）：number
      - `data.wrongQuestionCount`（可选）：integer
    - `traceId`（必填）：string

- `400`：Error response
  - `application/json`
    - `code`（必填）：string
    - `message`（必填）：string
    - `data`（可选）：null
    - `traceId`（必填）：string
