# Política de segurança

## Versões suportadas

Este projeto mantém apenas a versão mais recente disponível na branch `main`.

## Como reportar uma vulnerabilidade

Não abra uma issue pública com detalhes de uma vulnerabilidade, credenciais,
tokens, nomes de repositórios privados ou dados presentes em relatórios.

Use a opção **Report a vulnerability** na aba **Security** do repositório para
enviar o relato de forma privada. Se essa opção ainda não estiver disponível,
entre em contato com o mantenedor pelo perfil do GitHub e solicite um canal
privado antes de compartilhar detalhes técnicos.

Inclua, quando possível:

- descrição e impacto;
- passos mínimos para reprodução;
- versão ou commit afetado;
- sugestão de correção ou mitigação;
- confirmação de que nenhum segredo real foi publicado.

O recebimento será confirmado assim que possível. A correção e a divulgação
serão coordenadas de acordo com a gravidade e a complexidade do problema.

## Boas práticas para contribuidores

- Nunca faça commit de `.env`, tokens, chaves ou relatórios com dados internos.
- Use tokens de menor privilégio e com validade limitada.
- Revogue imediatamente qualquer credencial exposta.
- Revise atualizações automáticas e os resultados de CI e CodeQL antes do merge.
