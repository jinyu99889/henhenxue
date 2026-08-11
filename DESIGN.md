---
name: 狠狠学
description: Minimal video-led learning system interface
colors:
  ink: "#0a0a0b"
  paper: "#ffffff"
  soft-surface: "#f4f4f6"
  quiet-surface: "#ededee"
  muted-ink: "rgba(0,0,0,0.55)"
  hairline: "rgba(0,0,0,0.12)"
typography:
  display:
    fontFamily: "Inter, Arial, sans-serif"
    fontSize: "clamp(2rem, 8vw, 4.5rem)"
    fontWeight: 300
    lineHeight: 1
    letterSpacing: "-0.03em"
  body:
    fontFamily: "Inter, Arial, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.65
  label:
    fontFamily: "Inter, Arial, sans-serif"
    fontSize: "11px"
    fontWeight: 500
    lineHeight: 1.3
rounded:
  pill: "999px"
  circle: "50%"
spacing:
  compact: "8px"
  base: "16px"
  desktop-gutter: "32px"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    rounded: "{rounded.pill}"
    height: "38px"
    padding: "0 15px"
  chip:
    backgroundColor: "rgba(255,255,255,0.86)"
    textColor: "#343438"
    rounded: "{rounded.pill}"
    height: "27px"
    padding: "0 10px"
---

# Design System: 狠狠学

## Overview

**Creative North Star: "The Learning Aperture"**

狠狠学以全屏视频作为学习开始时的视觉证据，界面其余部分退到近黑、纸白与细边线。它不是仪表盘的缩略图，而是一个留出足够呼吸的入口：用户从一句明确主张进入博客、AI 知识树或个人题库。

所有控制件都是小尺度、可扫描的圆形或药丸；不使用装饰性渐变、数据卡片或伪技术网格。视频是页面唯一承载质感的媒介，文字与动作在白色渐隐带中保持直接和可读。阅读和操作页面延续纸白工作面，用横向图文带、细分隔线和可移动的工作区代替浮动卡片。

**Key Characteristics:**
- 视频主导，界面克制。
- 近黑和纸白承担全部层级。
- 大号轻字重标题与紧凑标签形成尺度对比。
- 交互只在明确入口、菜单和对话框上出现。

## Colors

高对比的纸白底色和近黑文字/动作色构成核心；灰阶仅用于降低非重点信息的存在感。

### Primary
- **Ink** (`#0a0a0b`): 主标题、图标圆点、主操作及深色导航药丸。

### Neutral
- **Paper** (`#ffffff`): 页面底色、渐隐底部和主要对话框。
- **Soft Surface** (`#f4f4f6`): 静态标签容器、关闭动作和次级导航。
- **Quiet Surface** (`#ededee`): 视频不可用时的舞台底色。
- **Hairline** (`rgba(0,0,0,0.12)`): 标签和菜单的低对比轮廓。

**The One-Medium Rule.** 首屏只有视频承担视觉质感；不可用彩色渐变、图案或带材质的 CSS 替代它。

## Typography

**Display Font:** Inter (with Arial fallback)

**Body Font:** Inter (with Arial fallback)

**Character:** 首页 Display 使用 Inter 的 300 字重和紧凑字距，以配合视频舞台；中文阅读与操作页面使用 Noto Sans SC（fallback 到平台中文无衬线），以 400-500 字重和正常字距保证笔画稳定、阅读清晰。

### Hierarchy
- **Display** (300, `clamp(2rem, 8vw, 4.5rem)`, 1): 首屏唯一主标题；桌面改为 `clamp(2.5rem, 5.5vw, 4.5rem)`。
- **Dialog title** (300, 28px, 1.1): 演示对话框的第二层标题。
- **Body** (400, 14px, 1.65): 功能说明与解释性文本。
- **Label** (500, 11px, 1.3): 导航、标签和辅助信息。
- **Chinese page title** (500, `clamp(2.05rem, 3.6vw, 3.35rem)`, 1.22): 内部工作台与阅读页面标题；不继承首页的超大轻字重样式。

## Layout

着陆页始终至少占满一个视口，并使用顶端固定导航、绝对定位视频和贴底内容的三层关系。移动端视频舞台以四周 10% 留白形成 80% 尺寸，底部内容纵向排列并使用 16px 边距；768px 起视频充满视口，导航和底部内容使用 32px 横向边距，底部切换为左右两端对齐。底部从透明过渡到纸白，保证大标题和操作在视频之上可读。

内部页面使用相同的固定导航和大号轻字重页标题，内容进入有约束的 1240px 工作面。博客列表在桌面端固定为左图右文的横向阅读带；知识树和题库使用细线分隔的多栏工作台。小于 768px 时工作面必须改为纵向顺序，不能横向压缩三栏内容。

## Elevation & Depth

默认表面平面化，层级来自 z-index、白色渐隐和边线。只有菜单与模态对话框使用漫射阴影，分别为 `0 18px 44px rgba(0,0,0,0.14)` 和 `0 20px 60px rgba(0,0,0,0.22)`，用于表示受控的临时前景层。

## Shapes

圆形只用于承载单个图标；药丸只用于短标签和清晰命令。菜单与模态作为工具面使用无圆角的矩形外框，避免把页面区段设计成卡片。边框为 1px 低对比近黑。

## Components

### Buttons
- **Shape:** 主操作和次操作为药丸（999px）；图标动作是圆形（50%）。
- **Primary:** Ink 底配 Paper 字，38px 高、`0 15px` 内距；悬停上移 2px。
- **Secondary:** 透明白底配 `rgba(0,0,0,0.35)` 描边，悬停增加白底不透明度。
- **Focus:** 所有原生按钮使用 2px Ink 可见焦点环及 3px 外偏移。

### Chips
- **Style:** Paper 半透明底、Hairline 边框、11px 标签字，27px 高。
- **State:** 悬停恢复为纯白，作为导航到对应原型入口的轻量动作。

### Navigation
- **Style:** 固定顶层，导航容器穿透点击，内部按钮恢复点击；桌面展示品牌字、模块标签和右侧标签，移动端隐藏文字保留具备 aria 名称的图标。

### Reading Strips and Workspaces
- **Reading strip:** 配图保持灰阶并占据左侧；文章元数据、标题、摘要和阅读动作在右侧以细分隔线组织。
- **Workspace:** 树、题库和对话由 1px Hairline 分隔，而不是通过嵌套卡片区分；只有短暂菜单或对话框可使用漫射阴影。

### Learning Aperture
- **Style:** 视频舞台位于最底层，使用灰阶与轻量白色 wash；底部渐隐带容纳首屏文案。
- **Motion:** 使用 `[0.16, 1, 0.3, 1]` 的缓出曲线。视频 1.8 秒淡入缩放，导航 0.8 秒下滑，底部内容按 0.5 至 1 秒错峰上移。

## Do's and Don'ts

### Do:
- **Do** 把真实视频作为首屏的主媒介，保持前景控件极少。
- **Do** 在移动端先保证标题、操作、标签与固定 AI 标注互不遮挡。
- **Do** 让每一个图标动作都有对应的 aria 名称和焦点态。

### Don't:
- **Don't** 在着陆页引入渐变文字、数据指标、卡片网格或技术风格的背景纹理。
- **Don't** 使用除灰阶以外的主题色来争夺视频和主标题的注意力。
- **Don't** 以隐藏文字替代无障碍名称。
