<%
' Classic ASP version of notes/form.html for comparison only.
'
' This file is not wired into the Spring Boot app. Thymeleaf resolves
' notes/form to notes/form.html, so this .asp file should not affect runtime.
'
' Expected Classic ASP view variables:
' form, mode, noteId, errors, csrfParameterName, csrfToken
'
' This sample assumes form and errors are Scripting.Dictionary-like objects.

Function H(value)
    If IsNull(value) Or IsEmpty(value) Then
        H = ""
    Else
        H = Server.HTMLEncode(CStr(value))
    End If
End Function

Function Field(source, name, fallback)
    On Error Resume Next
    Field = fallback

    If IsObject(source) Then
        If source.Exists(name) Then
            Field = source(name)
        End If
    End If

    If Err.Number <> 0 Then
        Err.Clear
        Field = fallback
    End If
    On Error GoTo 0
End Function

Function ErrorFor(errors, name)
    ErrorFor = Field(errors, name, "")
End Function

If IsEmpty(mode) Or mode = "" Then mode = "create"
If IsEmpty(csrfParameterName) Or csrfParameterName = "" Then csrfParameterName = "_csrf"
If IsEmpty(csrfToken) Then csrfToken = ""

Dim isCreate, pageTitle, heading, action, submitLabel, visibility
isCreate = (mode = "create")

If isCreate Then
    pageTitle = "노트 생성"
    heading = "새 노트 만들기"
    action = "/notes"
    submitLabel = "노트 생성"
Else
    pageTitle = "노트 수정"
    heading = "노트 수정"
    action = "/notes/" & Server.URLEncode(CStr(noteId)) & "/edit"
    submitLabel = "수정 저장"
End If

visibility = Field(form, "visibility", "PRIVATE")
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= H(pageTitle) %></title>
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
    <h1><%= H(heading) %></h1>

    <form class="panel form-wrap" action="<%= H(action) %>" method="post">
        <input type="hidden" name="<%= H(csrfParameterName) %>" value="<%= H(csrfToken) %>">

        <div class="row">
            <label for="title">제목</label>
            <input class="input"
                   id="title"
                   name="title"
                   type="text"
                   value="<%= H(Field(form, "title", "")) %>"
                   placeholder="노트 제목을 입력하세요">
            <% If ErrorFor(errors, "title") <> "" Then %>
                <div class="error"><%= H(ErrorFor(errors, "title")) %></div>
            <% End If %>
        </div>

        <div class="row">
            <label for="content">본문</label>
            <textarea class="input"
                      id="content"
                      name="content"
                      rows="12"
                      placeholder="기록하고 싶은 내용을 입력하세요"><%= H(Field(form, "content", "")) %></textarea>
            <% If ErrorFor(errors, "content") <> "" Then %>
                <div class="error"><%= H(ErrorFor(errors, "content")) %></div>
            <% End If %>
        </div>

        <div class="row two-cols">
            <div>
                <label for="visibility">공개 범위</label>
                <select class="input" id="visibility" name="visibility">
                    <option value="PRIVATE" <% If visibility = "PRIVATE" Then Response.Write "selected" %>>PRIVATE</option>
                    <option value="PUBLIC" <% If visibility = "PUBLIC" Then Response.Write "selected" %>>PUBLIC</option>
                </select>
            </div>
            <div>
                <label for="tagsText">태그 (콤마 구분)</label>
                <input class="input"
                       id="tagsText"
                       name="tagsText"
                       type="text"
                       value="<%= H(Field(form, "tagsText", "")) %>"
                       placeholder="kotlin, spring">
            </div>
        </div>

        <div class="actions">
            <button class="btn btn-primary" type="submit"><%= H(submitLabel) %></button>
            <a class="btn btn-ghost" href="/notes">목록으로</a>
        </div>
    </form>
</main>
</body>
</html>
