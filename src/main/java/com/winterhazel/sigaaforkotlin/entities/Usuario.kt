package com.winterhazel.sigaaforkotlin.entities

/**
 * Representa um usuário do SIGAA.
 * Este usuário pode ser o usuário atualmente logado ou algum usuário da página "Participantes" de
 * uma disciplina.
 *
 * @param email Para o participante de uma disciplina, é o e-mail cadastrado no SIGAA; para o
 * usuário logado, é uma string vazia, pois o portal do discente não mostra o e-mail completo e não
 * é possível abrir a página com os dados do usuário caso o status esteja como concluído. Para obter
 * este e-mail, você pode consultar os participantes de uma disciplina e identificar o usuário
 * logado pela matrícula.
 * @param curso Para o usuário logado, é o curso indicado no portal do discente; para o participante
 * de uma disciplina, é uma string vazia, pois a página de participantes não indica o curso.
 */
data class Usuario(
    val login: String,
    val matricula: Int?,
    val nome: String,
    val email: String,
    val urlAvatar: String,
    val curso: String,
    val tipoUsuario: Int,
    val disciplinasPeriodoAtual: ArrayList<Disciplina>
) {
    val isDiscente = tipoUsuario == USUARIO_DISCENTE
    val isDocente = tipoUsuario == USUARIO_DOCENTE

    companion object {
        const val USUARIO_DISCENTE = 1
        const val USUARIO_DOCENTE = 2
    }
}
