<div id="top"></div>

[![MIT License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]



<!-- PROJECT LOGO -->
<div align="center">

<h3 align="center">SIGAA for Kotlin</h3>

  <p align="center">
    API não oficial para a leitura otimizada de dados do SIGAA
    <br />
    <a href="https://imawa.github.io/sigaa-for-kotlin/"><strong>Documentação »</strong></a>
    <br />
  </p>
</div>



<!-- TABLE OF CONTENTS -->
<details>
  <summary>Tabela de Conteúdos</summary>
  <ol>
    <li>
      <a href="#sobre">Sobre</a>
      <ul>
        <li><a href="#funcionalidades">Funcionalidades</a></li>
        <li><a href="#tecnologias-utilizadas">Tecnologias utilizadas</a></li>
      </ul>
    </li>
    <li><a href="#instalação">Instalação</a></li>
    <li><a href="#uso">Uso</a></li>
    <li><a href="#licença">Licença</a></li>
    <li><a href="#contato">Contato</a></li>
    <li><a href="#agradecimentos">Agradecimentos</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## Sobre

Esta biblioteca possibilita a leitura de informações cadastradas no SIGAA (Sistema Integrado de Gestão de Atividades Acadêmicas) por meio de web scraping de maneira otimizada.

A API foi desenvolvida para o [SIGAA do Instituto Federal Catarinense](https://sig.ifc.edu.br/sigaa/), mas muito provavelmente pode ser adaptada para o sistema de outras instituições.

<p align="right">(<a href="#top">voltar ao topo</a>)</p>


### Funcionalidades

A API consegue obter do SIGAA:
- Arquivos enviados pelos docentes
- Anexos
- Aulas
- Avaliações
- Conteúdos
- Disciplinas (turmas virtuais)
- Frequência
- Informações do usuário logado
- Notas
- Notícias
- Participantes das turmas virtuais
- Questionários
- Referências
- Tarefas

<p align="right">(<a href="#top">voltar ao topo</a>)</p>

### Tecnologias utilizadas

* [OkHttp](https://github.com/square/okhttp/)
* [Timber](https://github.com/JakeWharton/timber/)
* [jsoup](https://github.com/jhy/jsoup/)

<p align="right">(<a href="#top">voltar ao topo</a>)</p>


<!-- GETTING STARTED -->
## Instalação

1. Clone o repositório para a pasta raiz do seu projeto
   ```sh
   git clone https://github.com/imawa/sigaa-for-kotlin.git
   ```
2. Adicione no `settings.gradle`
   ```groovy
   include ':sigaa-for-kotlin'
   ```
3. Adicione no `build.gradle` da sua aplicação
   ```groovy
   implementation project(path: ':sigaa-for-kotlin')
   ```

<p align="right">(<a href="#top">voltar ao topo</a>)</p>


<!-- USAGE EXAMPLES -->
## Uso

A interação com o sistema é feita por meio da classe SIGAA:
```kotlin
val sigaa = SIGAA(this)

if (sigaa.login("usuario", "senha")) {
    // Logado com sucesso
    for (disciplina in sigaa.getAllDisciplinas()) {
        println(sigaa.getNoticias(disciplina))
        println(sigaa.getTarefas(disciplina))
    }
} else {
    // Não foi possível logar
}
```

_Para mais exemplos, confira [/examples/](https://github.com/imawa/sigaa-for-kotlin/tree/master/examples)_

<p align="right">(<a href="#top">voltar ao topo</a>)</p>


<!-- LICENSE -->
## Licença

Distribuído sob a Licença MIT. Veja `LICENSE.txt` para mais informações.

<p align="right">(<a href="#top">voltar ao topo</a>)</p>



<!-- CONTACT -->
## Contato

Fabricio Duarte Júnior - fabricio.duarte.jr@gmail.com

<p align="right">(<a href="#top">voltar ao topo</a>)</p>



<!-- ACKNOWLEDGMENTS -->
## Agradecimentos

* [Best-README-Template](https://github.com/othneildrew/Best-README-Template/)

<p align="right">(<a href="#top">voltar ao topo</a>)</p>



<!-- MARKDOWN LINKS & IMAGES -->
[license-shield]: https://img.shields.io/github/license/imawa/sigaa-for-kotlin.svg?style=for-the-badge
[license-url]: https://github.com/imawa/sigaa-for-kotlin/blob/master/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/fabricio-duarte-júnior-676601231
