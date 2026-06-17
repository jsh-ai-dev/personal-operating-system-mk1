<?php
/**
 * PHP version of notes/form.html for comparison only.
 *
 * This file is not wired into the Spring Boot app. Thymeleaf resolves
 * notes/form to notes/form.html, so this .php file should not affect runtime.
 *
 * Expected PHP view variables:
 * $form, $mode, $noteId, $errors, $csrfParameterName, $csrfToken
 */

function form_e(mixed $value): string
{
    return htmlspecialchars((string) ($value ?? ''), ENT_QUOTES, 'UTF-8');
}

function form_field(mixed $value, string $name, mixed $default = null): mixed
{
    if (is_array($value)) {
        return $value[$name] ?? $default;
    }

    if (is_object($value)) {
        return $value->{$name} ?? $default;
    }

    return $default;
}

function form_error(array $errors, string $field): ?string
{
    return $errors[$field] ?? null;
}

$form = $form ?? [];
$mode = $mode ?? 'create';
$noteId = $noteId ?? null;
$errors = $errors ?? [];
$csrfParameterName = $csrfParameterName ?? '_csrf';
$csrfToken = $csrfToken ?? '';

$isCreate = $mode === 'create';
$pageTitle = $isCreate ? '노트 생성' : '노트 수정';
$heading = $isCreate ? '새 노트 만들기' : '노트 수정';
$action = $isCreate ? '/notes' : '/notes/' . rawurlencode((string) $noteId) . '/edit';
$submitLabel = $isCreate ? '노트 생성' : '수정 저장';
$visibility = form_field($form, 'visibility', 'PRIVATE');
?>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><?= form_e($pageTitle) ?></title>
    <link rel="stylesheet" href="/css/notes.css">
    <script defer src="/js/notes-theme.js"></script>
</head>
<body>
<div class="site-shell">
    <header class="site-header">
        <a class="brand" href="/">Personal OS</a>
        <nav class="top-nav">
            <div class="nav-group">
                <a class="nav-link" href="/notes">노트 목록</a>
                <a class="nav-link nav-link-primary" href="/notes/new">새 노트</a>
                <a class="nav-link" href="/summary">AI 요약</a>
            </div>
            <button id="themeToggle" class="btn btn-ghost" type="button" aria-label="테마 전환">다크모드</button>
        </nav>
    </header>
</div>

<main class="site-shell page-stack">
    <h1><?= form_e($heading) ?></h1>

    <form class="panel form-wrap" action="<?= form_e($action) ?>" method="post">
        <input type="hidden" name="<?= form_e($csrfParameterName) ?>" value="<?= form_e($csrfToken) ?>">

        <div class="row">
            <label for="title">제목</label>
            <input class="input"
                   id="title"
                   name="title"
                   type="text"
                   value="<?= form_e(form_field($form, 'title')) ?>"
                   placeholder="노트 제목을 입력하세요">
            <?php if ($error = form_error($errors, 'title')): ?>
                <div class="error"><?= form_e($error) ?></div>
            <?php endif; ?>
        </div>

        <div class="row">
            <label for="content">본문</label>
            <textarea class="input"
                      id="content"
                      name="content"
                      rows="12"
                      placeholder="기록하고 싶은 내용을 입력하세요"><?= form_e(form_field($form, 'content')) ?></textarea>
            <?php if ($error = form_error($errors, 'content')): ?>
                <div class="error"><?= form_e($error) ?></div>
            <?php endif; ?>
        </div>

        <div class="row two-cols">
            <div>
                <label for="visibility">공개 범위</label>
                <select class="input" id="visibility" name="visibility">
                    <option value="PRIVATE" <?= $visibility === 'PRIVATE' ? 'selected' : '' ?>>PRIVATE</option>
                    <option value="PUBLIC" <?= $visibility === 'PUBLIC' ? 'selected' : '' ?>>PUBLIC</option>
                </select>
            </div>
            <div>
                <label for="tagsText">태그 (콤마 구분)</label>
                <input class="input"
                       id="tagsText"
                       name="tagsText"
                       type="text"
                       value="<?= form_e(form_field($form, 'tagsText')) ?>"
                       placeholder="kotlin, spring">
            </div>
        </div>

        <div class="actions">
            <button class="btn btn-primary" type="submit"><?= form_e($submitLabel) ?></button>
            <a class="btn btn-ghost" href="/notes">목록으로</a>
        </div>
    </form>
</main>
</body>
</html>
