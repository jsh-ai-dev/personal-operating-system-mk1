package com.jsh.pos.application.port.`in`

/**
 * 노트 삭제 유스케이스의 계약입니다.
 *
 * 삭제는 "존재하는 리소스를 제거"하는 동작이므로,
 * 유스케이스 결과를 Boolean으로 반환해 컨트롤러가
 * 204(No Content) / 404(Not Found)를 결정할 수 있게 합니다.
 */
interface DeleteNoteUseCase {
    /**
     * ID로 노트를 삭제합니다.
     *
     * @param id 삭제할 노트의 ID
     * @return 삭제 성공 여부 (true: 삭제됨, false: 대상 없음)
     */
    fun deleteById(id: String): Boolean
}

