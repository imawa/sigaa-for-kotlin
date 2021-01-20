# sigaa_ifc

API não oficial para o SIGAA IFC (Sistema Integrado de Gestão de Atividades Acadêmicas) escrita em Java para o projeto de pesquisa "APP IFC campus Brusque: Uma proposta para organização da rotina escolar". Possívelmente também funciona no sistema dos outros campi.  

### Exemplo

Funções que envolvem alguma operação relacionado à rede (Sessao) devem ser feitas fora da main thread.

```java
Sessao s = new Sessao("http://sig.ifc.edu.br/"); //Criar sessão (não envolve rede)

Usuario u = s.login("usuario", "senha123"); //Logar a sessão e salvar usuário
if(u != null) { //O retorno será null se ocorreu algum erro durante o login, como usuário e/ou senha inválido(s), problemas de conexão ou SIGAA em manutenção

    if(u.disciplinas().size() > 0) { //Se houver mais de uma disciplina
    
        //Notas
        ArrayList<Nota> notas = s.disciplinaPegarNotas(u.disciplinas().get(0)); //Pega as notas da primeira disciplina da lista
        for(Nota n : notas) { //Printa abreviação e nota das notas disponíveis
            System.out.println(n.abrev() + " " + n.nota());
        }
        
        //Tarefas
        ArrayList<Tarefa> tarefas = a.disciplinaPegarTarefas(u.disciplinas().get(0)); //Pega as tarefas da primeira disciplina
        for(Tarefa t : tarefas) { //Printa as informações de cada tarefa
             System.out.println(t.corrigida() + " " + t.titulo() + " " + t.envios() + " " + t.enviavel() + " " + t.enviada() + " " + t.urlArquivo() + "\n" + t.descricao());

            if(t.contemArquivo()) { //Abrir o navegador para baixar o arquivo, se conter
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(t.urlArquivo()));
                startActivity(browserIntent);
            }
        }
        
    }
    
}
```
