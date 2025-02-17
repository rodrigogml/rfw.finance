package br.eng.rodrigogml.rfw.finance;

import java.time.format.DateTimeFormatter;

import br.eng.rodrigogml.rfw.kernel.RFW;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.preprocess.PreProcess;
import br.eng.rodrigogml.rfw.kernel.utils.RUString;

/**
 * Description: Esta classe representa um arquivo CNAB240.<br>
 * Seus m�todos permitem que dados sejam adicionados para a gera��o r�pida do arquivo compat�vel CNAB240.<br>
 * Por se tratar do sistema financeiro brasileiro esta classe utilizar� nomes em portugu�s para simplificar a associa��o aos instrumentos financeiros brasileiros. <Br>
 * <bR>
 * <br>
 * <b>Resumo dos padr�es do arquivo:</b>
 * <li>Campos n�mericos devem sempre estar alinhados a direita e preenchidos com 0 a esquerda, n�o utilizar espa�os.
 * <li>Campos Altanum�ricos devem sempre estar alinhados a esuqerda e preenchidos com brancos a direita.
 *
 * @author Rodrigo Leit�o
 * @since (14 de fev. de 2025)
 */
public class CNAB240 {

  /**
   * G001 C�digo do Banco na Compensa��o<br>
   * C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os. <br>
   * Preencher com �988� quando a transfer�ncia for efetuada para outra institui��o financeira utilizando o c�digo ISPB. Neste caso, dever� ser preenchido o c�digo ISPB no campo 26.3B.
   */
  private String codigoBanco = null;

  /**
   * G005 Tipo de Inscri��o da Empresa <br>
   * C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental.Dom�nio:
   * <ul>
   * <li>'0' = Isento / N�o Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo � obrigat�rio para DOC e TED (Forma de Lan�amento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com cr�dito em conta, o CPF dever� ser do 1� titular
   * <li>Para o Produto/Servi�o Cobran�a considerar como obrigat�rio, a partir de 01.06.2015, somente o CPF (c�digo 1) ou o CNPJ (c�digo 2). Os demais c�digos n�o dever�o ser utilizados.
   */
  private String tipoInscricao = null;

  /**
   * G006 N�mero de Inscri��o da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}.<br>
   * N�mero de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental. <br>
   * Quando o Tipo de Inscri��o for igual a zero (n�o informado), preencher com zeros.
   */
  private String numeroInscricao;

  /**
   * G007 C�digo do Conv�nio no Banco<br>
   * C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   */
  private String codigoConvenio;

  /**
   * G008 Ag�ncia Mantenedora da Conta<br>
   * C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente.<Br>
   * Ref�re-se � ag�ncia da conta que receber� o arquivo de lote.
   */
  private String agencia;

  /**
   * G009 D�gito Verificador da Ag�ncia<Br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia.<br>
   * N�o definir se o banco n�o utilizar DV para a ag�ncia.
   *
   */
  private String agenciaDV;

  /**
   * G010 N�mero da Conta Corrente<br>
   * N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   */
  private String contaNumero;

  /**
   * G011 D�gito Verificador da Conta<br>
   * C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 1� posi��o deste d�gito.<br>
   * Exemplo :
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Conta = 3
   * </ul>
   * <b>ATEN��O:</B>A maioria doa bancos n�o utiliza esse DV de conta separado, mas sim o DV do conjunto Ag�ncia + Conta, que deve ser definido no {@link #agenciaContaDV}.
   */
  private String contaDV;

  /**
   * G012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 2� posi��o deste d�gito.<br>
   * Exemplo:
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Ag/Conta = 6
   * </ul>
   */
  private String agenciaContaDV;

  /**
   * G013 Nome<br>
   * Nome que identifica a pessoa, f�sica ou jur�dica, a qual se quer fazer refer�ncia.
   *
   */
  private String nomeEmpresa;

  /**
   * G014 Nome do Banco<br>
   * Nome que identifica o Banco que est� recebendo ou enviando o arquivo.
   */
  private String nomeBanco;

  /**
   * G018 N�mero Seq�encial do Arquivo<br>
   * N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados. <br>
   * Evoluir um n�mero seq�encial a cada header de arquivo.
   */
  private String numeroSequencial;

  /**
   * G022 Para Uso Reservado da Empresa<br>
   * Texto de observa��es destinado para uso exclusivo da Empresa.
   */
  private String reservadoEmpresa;

  /**
   * Inicia uma nova inst�ncia, que representar� um novo arquivo CNAB240.
   */
  public CNAB240() {
  }

  /**
   * Recupera o conte�do do arquivo com todas as instru��es que foram montadas no objeto.
   *
   * @return Conte�do de arquivo no formato CNAB240 com as instru��es criadas.
   * @throws RFWException
   */
  public String getFileContent() throws RFWException {
    StringBuilder buff = new StringBuilder();

    // Valida atributos que s�o requeridos no Header e no Footer previamente para n�o ter que validar em todos os blocos, j� que ambos registros s�o obrigat�rios em todos os arquivos.
    PreProcess.requiredNonNullCritical(codigoBanco, "Voc� deve definir o atributo C�digo do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(tipoInscricao, "Voc� deve definir o atributo Tipo de Inscri��o para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(numeroInscricao, "Voc� deve definir o atributo N�mero de Inscri��o para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(agencia, "Voc� deve definir o atributo Ag�ncia para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(contaNumero, "Voc� deve definir o atributo N�mero da Conta para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(nomeEmpresa, "Voc� deve definir o atributo Nome da Empresa para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(nomeBanco, "Voc� deve definir o atributo Nome do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(numeroSequencial, "Voc� deve definir o atributo N�mero Sequencial para gerar o arquivo CNAB240.");

    buff.append(writeFileHeader());

    return buff.toString();
  }

  /**
   * Escreve o caba�alho do arquivo.
   *
   * @param buff Buffer para escrita do conte�do.
   * @return
   * @throws RFWException Lan�ado caso alguma informa��o n�o esteja definida.
   */
  private StringBuilder writeFileHeader() throws RFWException {
    StringBuilder buff = new StringBuilder();
    // C�digo do Banco na Compensa��o 1-3 3 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
    // Lote de Servi�o 4-7 4 - Num '0000'
    // +Tipo de Registro 8-8 1 - Num '0'
    // +Uso Exclusivo FEBRABAN / CNAB 9-17 9 - Alfa Brancos
    buff.append("00000         ");
    // Tipo de Inscri��o da Empresa 18-18 1 - Num
    buff.append(tipoInscricao);
    // N�mero de Inscri��o da Empresa 19-32 14 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14));
    // C�digo do Conv�nio no Banco 33-52 20 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20));
    // Ag�ncia Mantenedora da Conta 53-57 5 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5));
    // D�gito Verificador da Ag�ncia 58-58 1 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agenciaDV, 1));
    // N�mero da Conta Corrente 59-70 12 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", contaNumero, 12));
    // D�gito Verificador da Conta 71-71 1 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", contaDV, 1));
    // D�gito Verificador da Ag/Conta 72-72 1 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agenciaContaDV, 1));
    // Nome da Empresa 73-102 30 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30));
    // Nome do Banco 103-132 30 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeBanco, 30));
    // Uso Exclusivo FEBRABAN / CNAB 133-142 10 - Alfa Brancos
    // +C�digo Remessa / Retorno 143-143 1 - Num
    buff.append("          1");
    // Data de Gera��o do Arquivo 144-151 8 - Num
    // +Hora de Gera��o do Arquivo 152-157 6 - Num
    buff.append(RFW.getDateTime().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmss")));
    // N�mero Seq�encial do Arquivo 158 163 6 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroSequencial, 6));
    // No da Vers�o do Layout do Arquivo 164-166 3 - Num '103'
    // +Densidade de Grava��o do Arquivo 167-171 5 - Num
    // +Para Uso Reservado do Banco 172-191 20 - Alfa
    buff.append("10300000 ");
    // Para Uso Reservado da Empresa 192 211 20 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", reservadoEmpresa, 20)); // Para Uso Reservado da Empresa (192-211)
    // Uso Exclusivo FEBRABAN / CNAB 212 240 29 - Alfa Brancos
    buff.append("                             "); // Uso Exclusivo FEBRABAN / CNAB (212-240)

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Arquivo de Lote. A linha n�o ficou com 240 caracteres.");
    buff.append(buff).append("\r\n"); // Adiciona quebra de linha ao final
    return buff;

  }

  // ###############################################

  // PreProcess.requiredMatch(logradouroEmpresa, "[\\w ]{0,30}");
  // PreProcess.requiredMatch(numeroEnderecoEmpresa, "[\\d]{0,5}");
  // PreProcess.requiredMatch(complementoEmpresa, "[\\w ]{0,15}");
  // PreProcess.requiredMatch(cidadeEmpresa, "[\\w ]{0,20}");
  // PreProcess.requiredMatch(cepEmpresa, "[\\d]{0,8}");
  // PreProcess.requiredMatch(estadoEmpresa, "[\\w]{0,2}");

  /**
   * # g001 C�digo do Banco na Compensa��o<br>
   * C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os. <br>
   * Preencher com �988� quando a transfer�ncia for efetuada para outra institui��o financeira utilizando o c�digo ISPB. Neste caso, dever� ser preenchido o c�digo ISPB no campo 26.3B.
   *
   * @return the g001 C�digo do Banco na Compensa��o<br>
   *         C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os
   */
  public String getCodigoBanco() {
    return codigoBanco;
  }

  /**
   * # g001 C�digo do Banco na Compensa��o<br>
   * C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os. <br>
   * Preencher com �988� quando a transfer�ncia for efetuada para outra institui��o financeira utilizando o c�digo ISPB. Neste caso, dever� ser preenchido o c�digo ISPB no campo 26.3B.
   *
   * @param codigoBanco the new g001 C�digo do Banco na Compensa��o<br>
   *          C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os
   * @throws RFWException
   */
  public void setCodigoBanco(String codigoBanco) throws RFWException {
    PreProcess.requiredNonNullMatch(codigoBanco, "\\d{3}");
    this.codigoBanco = codigoBanco;
  }

  /**
   * # g005 Tipo de Inscri��o da Empresa <br>
   * C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental.Dom�nio:
   * <ul>
   * <li>'0' = Isento / N�o Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo � obrigat�rio para DOC e TED (Forma de Lan�amento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com cr�dito em conta, o CPF dever� ser do 1� titular
   * <li>Para o Produto/Servi�o Cobran�a considerar como obrigat�rio, a partir de 01.06.2015, somente o CPF (c�digo 1) ou o CNPJ (c�digo 2). Os demais c�digos n�o dever�o ser utilizados.
   *
   * @return the g005 Tipo de Inscri��o da Empresa <br>
   *         C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental
   */
  public String getTipoInscricao() {
    return tipoInscricao;
  }

  /**
   * # g005 Tipo de Inscri��o da Empresa <br>
   * C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental.Dom�nio:
   * <ul>
   * <li>'0' = Isento / N�o Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo � obrigat�rio para DOC e TED (Forma de Lan�amento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com cr�dito em conta, o CPF dever� ser do 1� titular
   * <li>Para o Produto/Servi�o Cobran�a considerar como obrigat�rio, a partir de 01.06.2015, somente o CPF (c�digo 1) ou o CNPJ (c�digo 2). Os demais c�digos n�o dever�o ser utilizados.
   *
   * @param tipoInscricao the new g005 Tipo de Inscri��o da Empresa <br>
   *          C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental
   * @throws RFWException
   */
  public void setTipoInscricao(String tipoInscricao) throws RFWException {
    PreProcess.requiredNonNullMatch(tipoInscricao, "[01239]");
    this.tipoInscricao = tipoInscricao;
  }

  /**
   * # g006 N�mero de Inscri��o da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}.<br>
   * N�mero de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental. <br>
   * Quando o Tipo de Inscri��o for igual a zero (n�o informado), preencher com zeros.
   *
   * @return the g006 N�mero de Inscri��o da Empresa<br>
   *         Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}
   */
  public String getNumeroInscricao() {
    return numeroInscricao;
  }

  /**
   * # g006 N�mero de Inscri��o da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}.<br>
   * N�mero de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental. <br>
   * Quando o Tipo de Inscri��o for igual a zero (n�o informado), preencher com zeros.
   *
   * @param numeroInscricao the new g006 N�mero de Inscri��o da Empresa<br>
   *          Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}
   */
  public void setNumeroInscricao(String numeroInscricao) throws RFWException {
    PreProcess.requiredNonNullMatch(numeroInscricao, "\\d{1,14}");
    this.numeroInscricao = numeroInscricao;
  }

  /**
   * # g007 C�digo do Conv�nio no Banco<br>
   * C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   * .
   *
   * @return the g007 C�digo do Conv�nio no Banco<br>
   *         C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente
   */
  public String getCodigoConvenio() {
    return codigoConvenio;
  }

  /**
   * # g007 C�digo do Conv�nio no Banco<br>
   * C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   * .
   *
   * @param codigoConvenio the new g007 C�digo do Conv�nio no Banco<br>
   *          C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente
   */
  public void setCodigoConvenio(String codigoConvenio) throws RFWException {
    PreProcess.requiredMatch(codigoConvenio, "[\\w ]{0,20}");
    this.codigoConvenio = codigoConvenio;
  }

  /**
   * # g008 Ag�ncia Mantenedora da Conta<br>
   * C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente.<Br>
   * Ref�re-se � ag�ncia da conta que receber� o arquivo de lote.
   *
   * @return the g008 Ag�ncia Mantenedora da Conta<br>
   *         C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente
   */
  public String getAgencia() {
    return agencia;
  }

  /**
   * # g008 Ag�ncia Mantenedora da Conta<br>
   * C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente.<Br>
   * Ref�re-se � ag�ncia da conta que receber� o arquivo de lote.
   *
   * @param agencia the new g008 Ag�ncia Mantenedora da Conta<br>
   *          C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente
   */
  public void setAgencia(String agencia) throws RFWException {
    PreProcess.requiredNonNullMatch(agencia, "\\d{1,5}");
    this.agencia = agencia;
  }

  /**
   * # g009 D�gito Verificador da Ag�ncia<Br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia.<br>
   * N�o definir se o banco n�o utilizar DV para a ag�ncia.
   *
   * @return the g009 D�gito Verificador da Ag�ncia<Br>
   *         C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia
   */
  public String getAgenciaDV() {
    return agenciaDV;
  }

  /**
   * # g009 D�gito Verificador da Ag�ncia<Br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia.<br>
   * N�o definir se o banco n�o utilizar DV para a ag�ncia.
   *
   * @param agenciaDV the new g009 D�gito Verificador da Ag�ncia<Br>
   *          C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia
   */
  public void setAgenciaDV(String agenciaDV) throws RFWException {
    PreProcess.requiredMatch(agenciaDV, "\\d{1}");
    this.agenciaDV = agenciaDV;
  }

  /**
   * # g010 N�mero da Conta Corrente<br>
   * N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   *
   * @return the g010 N�mero da Conta Corrente<br>
   *         N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente
   */
  public String getContaNumero() {
    return contaNumero;
  }

  /**
   * # g010 N�mero da Conta Corrente<br>
   * N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   *
   * @param contaNumero the new g010 N�mero da Conta Corrente<br>
   *          N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente
   */
  public void setContaNumero(String contaNumero) throws RFWException {
    PreProcess.requiredNonNullMatch(contaNumero, "\\d{1,12}");
    this.contaNumero = contaNumero;
  }

  /**
   * # g011 D�gito Verificador da Conta<br>
   * C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 1� posi��o deste d�gito.<br>
   * Exemplo :
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Conta = 3
   * </ul>
   * <b>ATEN��O:</B>A maioria doa bancos n�o utiliza esse DV de conta separado, mas sim o DV do conjunto Ag�ncia + Conta, que deve ser definido no {@link #agenciaContaDV}.
   *
   * @return the g011 D�gito Verificador da Conta<br>
   *         C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente
   */
  public String getContaDV() {
    return contaDV;
  }

  /**
   * # g011 D�gito Verificador da Conta<br>
   * C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 1� posi��o deste d�gito.<br>
   * Exemplo :
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Conta = 3
   * </ul>
   * <b>ATEN��O:</B>A maioria doa bancos n�o utiliza esse DV de conta separado, mas sim o DV do conjunto Ag�ncia + Conta, que deve ser definido no {@link #agenciaContaDV}.
   *
   * @param contaDV the new g011 D�gito Verificador da Conta<br>
   *          C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente
   */
  public void setContaDV(String contaDV) throws RFWException {
    PreProcess.requiredNonNullMatch(contaDV, "\\d{1}");
    this.contaDV = contaDV;
  }

  /**
   * # g012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 2� posi��o deste d�gito.<br>
   * Exemplo:
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Ag/Conta = 6
   * </ul>
   * .
   *
   * @return the g012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   *         C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente
   */
  public String getAgenciaContaDV() {
    return agenciaContaDV;
  }

  /**
   * # g012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 2� posi��o deste d�gito.<br>
   * Exemplo:
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Ag/Conta = 6
   * </ul>
   * .
   *
   * @param agenciaContaDV the new g012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   *          C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente
   */
  public void setAgenciaContaDV(String agenciaContaDV) throws RFWException {
    PreProcess.requiredMatch(agenciaContaDV, "\\d{1}");
    this.agenciaContaDV = agenciaContaDV;
  }

  /**
   * # g013 Nome<br>
   * Nome que identifica a pessoa, f�sica ou jur�dica, a qual se quer fazer refer�ncia.
   *
   * @return the g013 Nome<br>
   *         Nome que identifica a pessoa, f�sica ou jur�dica, a qual se quer fazer refer�ncia
   */
  public String getNomeEmpresa() {
    return nomeEmpresa;
  }

  /**
   * # g013 Nome<br>
   * Nome que identifica a pessoa, f�sica ou jur�dica, a qual se quer fazer refer�ncia.
   *
   * @param nomeEmpresa the new g013 Nome<br>
   *          Nome que identifica a pessoa, f�sica ou jur�dica, a qual se quer fazer refer�ncia
   */
  public void setNomeEmpresa(String nomeEmpresa) throws RFWException {
    PreProcess.requiredMatch(nomeEmpresa, "[\\w ]{0,30}");
    this.nomeEmpresa = nomeEmpresa;
  }

  /**
   * # g014 Nome do Banco<br>
   * Nome que identifica o Banco que est� recebendo ou enviando o arquivo.
   *
   * @return the g014 Nome do Banco<br>
   *         Nome que identifica o Banco que est� recebendo ou enviando o arquivo
   */
  public String getNomeBanco() {
    return nomeBanco;
  }

  /**
   * # g014 Nome do Banco<br>
   * Nome que identifica o Banco que est� recebendo ou enviando o arquivo.
   *
   * @param nomeBanco the new g014 Nome do Banco<br>
   *          Nome que identifica o Banco que est� recebendo ou enviando o arquivo
   */
  public void setNomeBanco(String nomeBanco) throws RFWException {
    PreProcess.requiredMatch(nomeBanco, "[\\w ]{0,30}");
    this.nomeBanco = nomeBanco;
  }

  /**
   * # g018 N�mero Seq�encial do Arquivo<br>
   * N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados. <br>
   * Evoluir um n�mero seq�encial a cada header de arquivo.
   *
   * @return the g018 N�mero Seq�encial do Arquivo<br>
   *         N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados
   */
  public String getNumeroSequencial() {
    return numeroSequencial;
  }

  /**
   * # g018 N�mero Seq�encial do Arquivo<br>
   * N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados. <br>
   * Evoluir um n�mero seq�encial a cada header de arquivo.
   *
   * @param numeroSequencial the new g018 N�mero Seq�encial do Arquivo<br>
   *          N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados
   */
  public void setNumeroSequencial(String numeroSequencial) throws RFWException {
    PreProcess.requiredNonNullMatch(numeroSequencial, "\\d{1,6}");
    this.numeroSequencial = numeroSequencial;
  }

  /**
   * # g022 Para Uso Reservado da Empresa<br>
   * Texto de observa��es destinado para uso exclusivo da Empresa.
   *
   * @return the g022 Para Uso Reservado da Empresa<br>
   *         Texto de observa��es destinado para uso exclusivo da Empresa
   */
  public String getReservadoEmpresa() {
    return reservadoEmpresa;
  }

  /**
   * # g022 Para Uso Reservado da Empresa<br>
   * Texto de observa��es destinado para uso exclusivo da Empresa.
   *
   * @param reservadoEmpresa the new g022 Para Uso Reservado da Empresa<br>
   *          Texto de observa��es destinado para uso exclusivo da Empresa
   */
  public void setReservadoEmpresa(String reservadoEmpresa) throws RFWException {
    PreProcess.requiredMatch(reservadoEmpresa, "[\\w ]{0,30}");
    this.reservadoEmpresa = reservadoEmpresa;
  }

  // #####################################################################################################################################################################
  // #####################################################################################################################################################################
  // #####################################################################################################################################################################
  // #####################################################################################################################################################################
  // #####################################################################################################################################################################
  // #####################################################################################################################################################################

  // /**
  // * Enumera��o com os diferentes tipos de lotes suportados pelo arquivo.
  // */
  // private static enum TipoLote {
  // /**
  // * Bloco com pagamentos de boletos do mesmo banco.
  // */
  // TITULODECOBRANCA_MESMOBANCO,
  // /**
  // * Bloco com pagamentos de boletos de outros bancos.
  // */
  // TITULODECOBRANCA_OUTROBANCO,
  // }
  //
  // /**
  // * Classe para representar um bloco de Batch sendo criado
  // */
  // class DadosLote {
  // TipoLote tipoLote = null;
  // int numeroLote = -1;
  // int contadorRegistros = 0;
  // final StringBuilder buff = new StringBuilder();
  // }
  //
  // /**
  // * Buffer com o conte�do do cabe�alho do arquivo. Criado na inicializa��o da inst�ncia.
  // */
  // private StringBuilder buffHeaderArquivo = new StringBuilder();
  //
  // /**
  // * Hash/Lista com os buffers de cada Batch
  // */
  // private LinkedHashMap<TipoLote, DadosLote> lotes = new LinkedHashMap<TipoLote, DadosLote>();
  //
  // // ### Vari�veis do cabe�alho do arquivo utilizados em diferentes partes do arquivo
  // private String codigoBanco;
  // private String tipoInscricao;
  // private String numeroInscricao;
  // private String codigoConvenio;
  // private String agencia;
  // private String dvAgencia;
  // private String numeroConta;
  // private String dvConta;
  // private String dvAgenciaConta;
  // private String nomeEmpresa;
  // private String logradouroEmpresa;
  // private String numeroEnderecoEmpresa;
  // private String complementoEmpresa;
  // private String cidadeEmpresa;
  // private String cepEmpresa;
  // private String estadoEmpresa;
  //
  // public CNAB240(String codigoBanco, String tipoInscricao, String numeroInscricao, String codigoConvenio,
  // String agencia, String dvAgencia, String numeroConta, String dvConta, String dvAgenciaConta,
  // String nomeEmpresa, String nomeBanco, String numeroSequencial, String reservadoEmpresa,
  // String logradouroEmpresa, String numeroEnderecoEmpresa, String complementoEmpresa,
  // String cidadeEmpresa, String cepEmpresa, String estadoEmpresa) throws RFWException {
  //
  //
  //
  // this.codigoBanco = codigoBanco;
  // this.tipoInscricao = tipoInscricao;
  // this.numeroInscricao = numeroInscricao;
  // this.codigoConvenio = codigoConvenio;
  // this.agencia = agencia;
  // this.dvAgencia = dvAgencia;
  // this.numeroConta = numeroConta;
  // this.dvConta = dvConta;
  // this.dvAgenciaConta = dvAgenciaConta;
  // this.nomeEmpresa = nomeEmpresa;
  // // this.numeroSequencial = numeroSequencial;
  // // this.reservadoEmpresa = reservadoEmpresa;
  // this.logradouroEmpresa = logradouroEmpresa;
  // this.numeroEnderecoEmpresa = numeroEnderecoEmpresa;
  // this.complementoEmpresa = complementoEmpresa;
  // this.cidadeEmpresa = cidadeEmpresa;
  // this.cepEmpresa = cepEmpresa;
  // this.estadoEmpresa = estadoEmpresa;
  //
  // StringBuilder header = new StringBuilder();
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3)); // C�digo do Banco na Compensa��o (001-003)
  // header.append("0000"); // Lote de Servi�o (004-007)
  // header.append("0"); // Tipo de Registro (008-008)
  // header.append(RUString.repeatString(9, " ")); // Uso Exclusivo FEBRABAN / CNAB (009-017)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", tipoInscricao, 1)); // Tipo de Inscri��o da Empresa (018-018)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14)); // N�mero de Inscri��o da Empresa (019-032)
  // header.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20)); // C�digo do Conv�nio no Banco (033-052)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5)); // Ag�ncia Mantenedora da Conta (053-057)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgencia, 1)); // D�gito Verificador da Ag�ncia (058-058)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroConta, 12)); // N�mero da Conta Corrente (059-070)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvConta, 1)); // D�gito Verificador da Conta (071-071)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgenciaConta, 1)); // D�gito Verificador da Ag/Conta (072-072)
  // header.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30)); // Nome da Empresa (073-102)
  // header.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeBanco, 30)); // Nome do Banco (103-132)
  // header.append(RUString.repeatString(10, " ")); // Uso Exclusivo FEBRABAN / CNAB (133-142)
  // header.append("1"); // C�digo Remessa/Retorno (143-143)
  // header.append(RFW.getDateTime().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmss"))); // Data de Gera��o do Arquivo (144-151) + Hora de Gera��o do Arquivo (152-157)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroSequencial, 6)); // N�mero Sequencial do Arquivo (158-163)
  // header.append("103"); // N� da Vers�o do Layout do Arquivo (164-166)
  // header.append("00000"); // Densidade de Grava��o do Arquivo (167-171)
  // header.append(" "); // Para Uso Reservado do Banco (172-191)
  // header.append(RUString.completeOrTruncateUntilLengthRight(" ", reservadoEmpresa, 20)); // Para Uso Reservado da Empresa (192-211)
  // header.append(RUString.repeatString(29, " ")); // Uso Exclusivo FEBRABAN / CNAB (212-240)
  //
  // if (header.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Arquivo de Lote. A linha n�o ficou com 240 caracteres.");
  // buffHeaderArquivo.append(header).append("\r\n"); // Adiciona quebra de linha ao final
  // }
  //
  // /**
  // * Adiciona um t�tulo de cobran�a (boleto) de fornecedor (sem ser servi�o) ao arquivo.
  // *
  // * @param barCode C�digo de Barras do T�tulo
  // * @throws RFWException
  // */
  // public void incluirTituloCobranca(String barCode) throws RFWException {
  // PreProcess.requiredNonNullMatch(barCode, "\\d{44}");
  //
  // String bancoEmissor = barCode.substring(0, 3);
  // DadosLote dadosLote = null;
  // if (bancoEmissor.equals(this.codigoBanco)) {
  // dadosLote = this.lotes.get(TipoLote.TITULODECOBRANCA_MESMOBANCO);
  // if (dadosLote == null) dadosLote = criarLoteTitulosCobrancaMesmoBanco();
  // } else {
  // dadosLote = this.lotes.get(TipoLote.TITULODECOBRANCA_OUTROBANCO);
  // }
  //
  // StringBuilder buff = new StringBuilder();
  // buff.append(RUString.completeUntilLengthLeft("0", this.codigoBanco, 3)); // C�digo do Banco (j� definido no construtor) - Posi��o 1-3
  // buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(dadosLote.numeroLote), 4)); // Lote de Servi�o (deve ser incrementado a cada lote criado) - Posi��o 4-7
  // buff.append("3"); // Registro Tipo de Registro 8 8 1
  // buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(++dadosLote.contadorRegistros), 3)); // N� do Registro N� Seq�encial do Registro no Lote 9-13 5
  // buff.append("J"); // Segmento C�digo de Segmento no Reg. Detalhe 14-14 1
  // buff.append("000"); // Tipo Tipo de Movimento 15-15 1 + C�digo C�digo da Instru��o p/ Movimento 16-17 2
  // buff.append(barCode); // C�digo Barras C�digo de Barras 18 61 44
  //
  // }
  //
  // /**
  // * Abre o Lote para lan�amento de T�culos de Cobran�a para o mesmo banco
  // *
  // * @return
  // * @throws RFWException
  // */
  // private DadosLote criarLoteTitulosCobrancaMesmoBanco() throws RFWException {
  // DadosLote dadosLote = new DadosLote();
  // this.lotes.put(TipoLote.TITULODECOBRANCA_MESMOBANCO, dadosLote);
  // dadosLote.tipoLote = TipoLote.TITULODECOBRANCA_MESMOBANCO;
  // dadosLote.numeroLote = this.lotes.size();
  //
  // dadosLote.buff.append(RUString.completeUntilLengthLeft("0", this.codigoBanco, 3)); // C�digo do Banco (j� definido no construtor) - Posi��o 1-3
  // dadosLote.buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(dadosLote.numeroLote), 4)); // Lote de Servi�o (deve ser incrementado a cada lote criado) - Posi��o 4-7
  //
  // // Tipo de Registro (Sempre '1' para Header de Lote) - Posi��o 8-8
  // // Tipo de Opera��o - Posi��o 9-9
  // // Tipo de Servi�o - Posi��o 10-11 / '98' = Pagamentos Diversos
  // // Forma de Lan�amento - Posi��o 12-14 / '30' = Liquida��o de T�tulos do Pr�prio Banco
  // // Layout do Lote - Posi��o 14-16
  // // Uso Exclusivo FEBRABAN / CNAB - Posi��o 17-17 (Brancos)
  // dadosLote.buff.append("1C9830040 ");
  //
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", tipoInscricao, 1)); // Tipo de Inscri��o da Empresa (018-018)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14)); // N�mero de Inscri��o da Empresa (019-032)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20)); // C�digo do Conv�nio no Banco (033-052)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5)); // Ag�ncia Mantenedora da Conta (053-057)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgencia, 1)); // D�gito Verificador da Ag�ncia (058-058)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroConta, 12)); // N�mero da Conta Corrente (059-070)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvConta, 1)); // D�gito Verificador da Conta (071-071)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgenciaConta, 1)); // D�gito Verificador da Ag/Conta (072-072)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30)); // Nome da Empresa (073-102)
  // dadosLote.buff.append(" "); // Mensagem 1 - Posi��o 103-142
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", logradouroEmpresa, 30)); // Logradouro Nome da Rua, Av, P�a, Etc 143-172 30
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", numeroEnderecoEmpresa, 5)); // N�mero N�mero do Local 173-177 5
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", complementoEmpresa, 15)); // Complemento Casa, Apto, Sala, Etc 178-192 15
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", cidadeEmpresa, 20)); // Cidade Cidade 193-212 20
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", cepEmpresa, 8)); // CEP CEP 213-217 5 + Complemento CEP Complemento do CEP 218-220 3
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", estadoEmpresa, 2)); // Estado Sigla do Estado 221-222 2
  // dadosLote.buff.append(" "); // CNAB Uso Exclusivo da FEBRABAN/CNAB 223-230 8 + Ocorr�ncias C�digo das Ocorr�ncias p/ Retorno 231-240 10
  //
  // if (dadosLote.buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Lote de T�tulos de Cobran�a no Memso Banco. A linha n�o ficou com 240 caracteres.");
  // dadosLote.buff.append("\r\n"); // Adiciona quebra de linha ao final
  // return dadosLote;
  // }

}
