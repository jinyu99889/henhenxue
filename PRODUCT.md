# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

React 19 + Vite + Motion + plain CSS. This customer-facing prototype establishes the frontend direction before backend integration.

## Users

Registered learners building private knowledge trees and question libraries; visitors reading the administrator's public blog; one administrator publishing articles and managing the system.

## Product Purpose

狠狠学 combines personal blog reading, AI-guided knowledge trees, and private practice libraries so a learner can move from understanding a topic to retaining it through practice.

## Positioning

It turns a learner's original question into a versioned, explorable knowledge tree and links study with a personal, review-controlled question bank.

## Operating Context

Users study a topic with their own verified DeepSeek or DashScope credential, expand nodes with contextual follow-up questions, import MinerU Markdown ZIP exports, review generated questions, and practise across selected private libraries.

## Capabilities and Constraints

- The blog is administrator-only for publishing; visitors can browse and registered users can favorite.
- Knowledge trees and question libraries are private by owner and preserve immutable history snapshots.
- AI content uses only user-provided DeepSeek or DashScope credentials.
- The first UI deliverable is a landing-page prototype with representative mock navigation and no backend connection.

## Brand Commitments

The product name is 狠狠学. The homepage uses a minimal black-and-white, full-viewport video composition inspired by the supplied reference. Interface copy is concise Chinese, and the fixed AI assistance disclosure remains visible.

## Evidence on Hand

Product, technical, API, data-model, and frontend guidance live in `docs/`. The user supplied a CloudFront MP4 background video URL. No production article, tree, question, or user data is available, so the prototype uses clearly non-persistent demo content.

## Product Principles

- Make the first action obvious: learn, practise, or read.
- Let the user's own questions and materials remain the centre of the experience.
- Keep AI assistance transparent and under the user's control.
- Make progression feel focused rather than gamified.
