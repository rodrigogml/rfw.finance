package br.eng.rodrigogml.rfw.finance;

import java.time.format.DateTimeFormatter;

import br.eng.rodrigogml.rfw.kernel.RFW;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.preprocess.PreProcess;
import br.eng.rodrigogml.rfw.kernel.utils.RUString;

/**
 * Description: Esta classe representa um arquivo CNAB240.<br>
 * Seus métodos permitem que dados sejam adicionados para a geração rápida do arquivo compatível CNAB240.<br>
 * Por se tratar do sistema financeiro brasileiro esta classe utilizará nomes em português para simplificar a associação aos instrumentos financeiros brasileiros. <Br>
 * <bR>
 * <br>
 * <b>Resumo dos padrões do arquivo:</b>
 * <li>Campos númericos devem sempre estar alinhados a direita e preenchidos com 0 a esquerda, não utilizar espaços.
 * <li>Campos Altanuméricos devem sempre estar alinhados a esuqerda e preenchidos com brancos a direita.
 *
 * @author Rodrigo Leitão
 * @since (14 de fev. de 2025)
 */
public class CNAB240 {

  /**
   * G001 Código do Banco na Compensação<br>
   * Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços. <br>
   * Preencher com “988” quando a transferência for efetuada para outra instituição financeira utilizando o código ISPB. Neste caso, deverá ser preenchido o código ISPB no campo 26.3B.
   */
  private String codigoBanco = null;

  /**
   * G005 Tipo de Inscrição da Empresa <br>
   * Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental.Domínio:
   * <ul>
   * <li>'0' = Isento / Não Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo é obrigatório para DOC e TED (Forma de Lançamento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com crédito em conta, o CPF deverá ser do 1º titular
   * <li>Para o Produto/Serviço Cobrança considerar como obrigatório, a partir de 01.06.2015, somente o CPF (código 1) ou o CNPJ (código 2). Os demais códigos não deverão ser utilizados.
   */
  private String tipoInscricao = null;

  /**
   * G006 Número de Inscrição da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}.<br>
   * Número de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental. <br>
   * Quando o Tipo de Inscrição for igual a zero (não informado), preencher com zeros.
   */
  private String numeroInscricao;

  /**
   * G007 Código do Convênio no Banco<br>
   * Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   */
  private String codigoConvenio;

  /**
   * G008 Agência Mantenedora da Conta<br>
   * Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente.<Br>
   * Refêre-se à agência da conta que receberá o arquivo de lote.
   */
  private String agencia;

  /**
   * G009 Dígito Verificador da Agência<Br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência.<br>
   * Não definir se o banco não utilizar DV para a agência.
   *
   */
  private String agenciaDV;

  /**
   * G010 Número da Conta Corrente<br>
   * Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   */
  private String contaNumero;

  /**
   * G011 Dígito Verificador da Conta<br>
   * Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 1ª posição deste dígito.<br>
   * Exemplo :
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Conta = 3
   * </ul>
   * <b>ATENÇÃO:</B>A maioria doa bancos não utiliza esse DV de conta separado, mas sim o DV do conjunto Agência + Conta, que deve ser definido no {@link #agenciaContaDV}.
   */
  private String contaDV;

  /**
   * G012 Dígito Verificador da Agência / Conta Corrente<br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 2ª posição deste dígito.<br>
   * Exemplo:
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Ag/Conta = 6
   * </ul>
   */
  private String agenciaContaDV;

  /**
   * G013 Nome<br>
   * Nome que identifica a pessoa, física ou jurídica, a qual se quer fazer referência.
   *
   */
  private String nomeEmpresa;

  /**
   * G014 Nome do Banco<br>
   * Nome que identifica o Banco que está recebendo ou enviando o arquivo.
   */
  private String nomeBanco;

  /**
   * G018 Número Seqüencial do Arquivo<br>
   * Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados. <br>
   * Evoluir um número seqüencial a cada header de arquivo.
   */
  private String numeroSequencial;

  /**
   * G022 Para Uso Reservado da Empresa<br>
   * Texto de observações destinado para uso exclusivo da Empresa.
   */
  private String reservadoEmpresa;

  /**
   * Inicia uma nova instância, que representará um novo arquivo CNAB240.
   */
  public CNAB240() {
  }

  /**
   * Recupera o conteúdo do arquivo com todas as instruções que foram montadas no objeto.
   *
   * @return Conteúdo de arquivo no formato CNAB240 com as instruções criadas.
   * @throws RFWException
   */
  public String getFileContent() throws RFWException {
    StringBuilder buff = new StringBuilder();

    // Valida atributos que são requeridos no Header e no Footer previamente para não ter que validar em todos os blocos, já que ambos registros são obrigatórios em todos os arquivos.
    PreProcess.requiredNonNullCritical(codigoBanco, "Você deve definir o atributo Código do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(tipoInscricao, "Você deve definir o atributo Tipo de Inscrição para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(numeroInscricao, "Você deve definir o atributo Número de Inscrição para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(agencia, "Você deve definir o atributo Agência para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(contaNumero, "Você deve definir o atributo Número da Conta para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(nomeEmpresa, "Você deve definir o atributo Nome da Empresa para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(nomeBanco, "Você deve definir o atributo Nome do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(numeroSequencial, "Você deve definir o atributo Número Sequencial para gerar o arquivo CNAB240.");

    buff.append(writeFileHeader());

    return buff.toString();
  }

  /**
   * Escreve o cabaçalho do arquivo.
   *
   * @param buff Buffer para escrita do conteúdo.
   * @return
   * @throws RFWException Lançado caso alguma informação não esteja definida.
   */
  private StringBuilder writeFileHeader() throws RFWException {
    StringBuilder buff = new StringBuilder();
    // Código do Banco na Compensação 1-3 3 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
    // Lote de Serviço 4-7 4 - Num '0000'
    // +Tipo de Registro 8-8 1 - Num '0'
    // +Uso Exclusivo FEBRABAN / CNAB 9-17 9 - Alfa Brancos
    buff.append("00000         ");
    // Tipo de Inscrição da Empresa 18-18 1 - Num
    buff.append(tipoInscricao);
    // Número de Inscrição da Empresa 19-32 14 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14));
    // Código do Convênio no Banco 33-52 20 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20));
    // Agência Mantenedora da Conta 53-57 5 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5));
    // Dígito Verificador da Agência 58-58 1 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agenciaDV, 1));
    // Número da Conta Corrente 59-70 12 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", contaNumero, 12));
    // Dígito Verificador da Conta 71-71 1 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", contaDV, 1));
    // Dígito Verificador da Ag/Conta 72-72 1 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agenciaContaDV, 1));
    // Nome da Empresa 73-102 30 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30));
    // Nome do Banco 103-132 30 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeBanco, 30));
    // Uso Exclusivo FEBRABAN / CNAB 133-142 10 - Alfa Brancos
    // +Código Remessa / Retorno 143-143 1 - Num
    buff.append("          1");
    // Data de Geração do Arquivo 144-151 8 - Num
    // +Hora de Geração do Arquivo 152-157 6 - Num
    buff.append(RFW.getDateTime().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmss")));
    // Número Seqüencial do Arquivo 158 163 6 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroSequencial, 6));
    // No da Versão do Layout do Arquivo 164-166 3 - Num '103'
    // +Densidade de Gravação do Arquivo 167-171 5 - Num
    // +Para Uso Reservado do Banco 172-191 20 - Alfa
    buff.append("10300000 ");
    // Para Uso Reservado da Empresa 192 211 20 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", reservadoEmpresa, 20)); // Para Uso Reservado da Empresa (192-211)
    // Uso Exclusivo FEBRABAN / CNAB 212 240 29 - Alfa Brancos
    buff.append("                             "); // Uso Exclusivo FEBRABAN / CNAB (212-240)

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Arquivo de Lote. A linha não ficou com 240 caracteres.");
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
   * # g001 Código do Banco na Compensação<br>
   * Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços. <br>
   * Preencher com “988” quando a transferência for efetuada para outra instituição financeira utilizando o código ISPB. Neste caso, deverá ser preenchido o código ISPB no campo 26.3B.
   *
   * @return the g001 Código do Banco na Compensação<br>
   *         Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços
   */
  public String getCodigoBanco() {
    return codigoBanco;
  }

  /**
   * # g001 Código do Banco na Compensação<br>
   * Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços. <br>
   * Preencher com “988” quando a transferência for efetuada para outra instituição financeira utilizando o código ISPB. Neste caso, deverá ser preenchido o código ISPB no campo 26.3B.
   *
   * @param codigoBanco the new g001 Código do Banco na Compensação<br>
   *          Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços
   * @throws RFWException
   */
  public void setCodigoBanco(String codigoBanco) throws RFWException {
    PreProcess.requiredNonNullMatch(codigoBanco, "\\d{3}");
    this.codigoBanco = codigoBanco;
  }

  /**
   * # g005 Tipo de Inscrição da Empresa <br>
   * Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental.Domínio:
   * <ul>
   * <li>'0' = Isento / Não Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo é obrigatório para DOC e TED (Forma de Lançamento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com crédito em conta, o CPF deverá ser do 1º titular
   * <li>Para o Produto/Serviço Cobrança considerar como obrigatório, a partir de 01.06.2015, somente o CPF (código 1) ou o CNPJ (código 2). Os demais códigos não deverão ser utilizados.
   *
   * @return the g005 Tipo de Inscrição da Empresa <br>
   *         Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental
   */
  public String getTipoInscricao() {
    return tipoInscricao;
  }

  /**
   * # g005 Tipo de Inscrição da Empresa <br>
   * Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental.Domínio:
   * <ul>
   * <li>'0' = Isento / Não Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo é obrigatório para DOC e TED (Forma de Lançamento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com crédito em conta, o CPF deverá ser do 1º titular
   * <li>Para o Produto/Serviço Cobrança considerar como obrigatório, a partir de 01.06.2015, somente o CPF (código 1) ou o CNPJ (código 2). Os demais códigos não deverão ser utilizados.
   *
   * @param tipoInscricao the new g005 Tipo de Inscrição da Empresa <br>
   *          Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental
   * @throws RFWException
   */
  public void setTipoInscricao(String tipoInscricao) throws RFWException {
    PreProcess.requiredNonNullMatch(tipoInscricao, "[01239]");
    this.tipoInscricao = tipoInscricao;
  }

  /**
   * # g006 Número de Inscrição da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}.<br>
   * Número de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental. <br>
   * Quando o Tipo de Inscrição for igual a zero (não informado), preencher com zeros.
   *
   * @return the g006 Número de Inscrição da Empresa<br>
   *         Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}
   */
  public String getNumeroInscricao() {
    return numeroInscricao;
  }

  /**
   * # g006 Número de Inscrição da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}.<br>
   * Número de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental. <br>
   * Quando o Tipo de Inscrição for igual a zero (não informado), preencher com zeros.
   *
   * @param numeroInscricao the new g006 Número de Inscrição da Empresa<br>
   *          Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}
   */
  public void setNumeroInscricao(String numeroInscricao) throws RFWException {
    PreProcess.requiredNonNullMatch(numeroInscricao, "\\d{1,14}");
    this.numeroInscricao = numeroInscricao;
  }

  /**
   * # g007 Código do Convênio no Banco<br>
   * Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   * .
   *
   * @return the g007 Código do Convênio no Banco<br>
   *         Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente
   */
  public String getCodigoConvenio() {
    return codigoConvenio;
  }

  /**
   * # g007 Código do Convênio no Banco<br>
   * Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   * .
   *
   * @param codigoConvenio the new g007 Código do Convênio no Banco<br>
   *          Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente
   */
  public void setCodigoConvenio(String codigoConvenio) throws RFWException {
    PreProcess.requiredMatch(codigoConvenio, "[\\w ]{0,20}");
    this.codigoConvenio = codigoConvenio;
  }

  /**
   * # g008 Agência Mantenedora da Conta<br>
   * Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente.<Br>
   * Refêre-se à agência da conta que receberá o arquivo de lote.
   *
   * @return the g008 Agência Mantenedora da Conta<br>
   *         Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente
   */
  public String getAgencia() {
    return agencia;
  }

  /**
   * # g008 Agência Mantenedora da Conta<br>
   * Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente.<Br>
   * Refêre-se à agência da conta que receberá o arquivo de lote.
   *
   * @param agencia the new g008 Agência Mantenedora da Conta<br>
   *          Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente
   */
  public void setAgencia(String agencia) throws RFWException {
    PreProcess.requiredNonNullMatch(agencia, "\\d{1,5}");
    this.agencia = agencia;
  }

  /**
   * # g009 Dígito Verificador da Agência<Br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência.<br>
   * Não definir se o banco não utilizar DV para a agência.
   *
   * @return the g009 Dígito Verificador da Agência<Br>
   *         Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência
   */
  public String getAgenciaDV() {
    return agenciaDV;
  }

  /**
   * # g009 Dígito Verificador da Agência<Br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência.<br>
   * Não definir se o banco não utilizar DV para a agência.
   *
   * @param agenciaDV the new g009 Dígito Verificador da Agência<Br>
   *          Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência
   */
  public void setAgenciaDV(String agenciaDV) throws RFWException {
    PreProcess.requiredMatch(agenciaDV, "\\d{1}");
    this.agenciaDV = agenciaDV;
  }

  /**
   * # g010 Número da Conta Corrente<br>
   * Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   *
   * @return the g010 Número da Conta Corrente<br>
   *         Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente
   */
  public String getContaNumero() {
    return contaNumero;
  }

  /**
   * # g010 Número da Conta Corrente<br>
   * Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   *
   * @param contaNumero the new g010 Número da Conta Corrente<br>
   *          Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente
   */
  public void setContaNumero(String contaNumero) throws RFWException {
    PreProcess.requiredNonNullMatch(contaNumero, "\\d{1,12}");
    this.contaNumero = contaNumero;
  }

  /**
   * # g011 Dígito Verificador da Conta<br>
   * Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 1ª posição deste dígito.<br>
   * Exemplo :
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Conta = 3
   * </ul>
   * <b>ATENÇÃO:</B>A maioria doa bancos não utiliza esse DV de conta separado, mas sim o DV do conjunto Agência + Conta, que deve ser definido no {@link #agenciaContaDV}.
   *
   * @return the g011 Dígito Verificador da Conta<br>
   *         Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente
   */
  public String getContaDV() {
    return contaDV;
  }

  /**
   * # g011 Dígito Verificador da Conta<br>
   * Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 1ª posição deste dígito.<br>
   * Exemplo :
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Conta = 3
   * </ul>
   * <b>ATENÇÃO:</B>A maioria doa bancos não utiliza esse DV de conta separado, mas sim o DV do conjunto Agência + Conta, que deve ser definido no {@link #agenciaContaDV}.
   *
   * @param contaDV the new g011 Dígito Verificador da Conta<br>
   *          Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente
   */
  public void setContaDV(String contaDV) throws RFWException {
    PreProcess.requiredNonNullMatch(contaDV, "\\d{1}");
    this.contaDV = contaDV;
  }

  /**
   * # g012 Dígito Verificador da Agência / Conta Corrente<br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 2ª posição deste dígito.<br>
   * Exemplo:
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Ag/Conta = 6
   * </ul>
   * .
   *
   * @return the g012 Dígito Verificador da Agência / Conta Corrente<br>
   *         Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente
   */
  public String getAgenciaContaDV() {
    return agenciaContaDV;
  }

  /**
   * # g012 Dígito Verificador da Agência / Conta Corrente<br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 2ª posição deste dígito.<br>
   * Exemplo:
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Ag/Conta = 6
   * </ul>
   * .
   *
   * @param agenciaContaDV the new g012 Dígito Verificador da Agência / Conta Corrente<br>
   *          Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente
   */
  public void setAgenciaContaDV(String agenciaContaDV) throws RFWException {
    PreProcess.requiredMatch(agenciaContaDV, "\\d{1}");
    this.agenciaContaDV = agenciaContaDV;
  }

  /**
   * # g013 Nome<br>
   * Nome que identifica a pessoa, física ou jurídica, a qual se quer fazer referência.
   *
   * @return the g013 Nome<br>
   *         Nome que identifica a pessoa, física ou jurídica, a qual se quer fazer referência
   */
  public String getNomeEmpresa() {
    return nomeEmpresa;
  }

  /**
   * # g013 Nome<br>
   * Nome que identifica a pessoa, física ou jurídica, a qual se quer fazer referência.
   *
   * @param nomeEmpresa the new g013 Nome<br>
   *          Nome que identifica a pessoa, física ou jurídica, a qual se quer fazer referência
   */
  public void setNomeEmpresa(String nomeEmpresa) throws RFWException {
    PreProcess.requiredMatch(nomeEmpresa, "[\\w ]{0,30}");
    this.nomeEmpresa = nomeEmpresa;
  }

  /**
   * # g014 Nome do Banco<br>
   * Nome que identifica o Banco que está recebendo ou enviando o arquivo.
   *
   * @return the g014 Nome do Banco<br>
   *         Nome que identifica o Banco que está recebendo ou enviando o arquivo
   */
  public String getNomeBanco() {
    return nomeBanco;
  }

  /**
   * # g014 Nome do Banco<br>
   * Nome que identifica o Banco que está recebendo ou enviando o arquivo.
   *
   * @param nomeBanco the new g014 Nome do Banco<br>
   *          Nome que identifica o Banco que está recebendo ou enviando o arquivo
   */
  public void setNomeBanco(String nomeBanco) throws RFWException {
    PreProcess.requiredMatch(nomeBanco, "[\\w ]{0,30}");
    this.nomeBanco = nomeBanco;
  }

  /**
   * # g018 Número Seqüencial do Arquivo<br>
   * Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados. <br>
   * Evoluir um número seqüencial a cada header de arquivo.
   *
   * @return the g018 Número Seqüencial do Arquivo<br>
   *         Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados
   */
  public String getNumeroSequencial() {
    return numeroSequencial;
  }

  /**
   * # g018 Número Seqüencial do Arquivo<br>
   * Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados. <br>
   * Evoluir um número seqüencial a cada header de arquivo.
   *
   * @param numeroSequencial the new g018 Número Seqüencial do Arquivo<br>
   *          Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados
   */
  public void setNumeroSequencial(String numeroSequencial) throws RFWException {
    PreProcess.requiredNonNullMatch(numeroSequencial, "\\d{1,6}");
    this.numeroSequencial = numeroSequencial;
  }

  /**
   * # g022 Para Uso Reservado da Empresa<br>
   * Texto de observações destinado para uso exclusivo da Empresa.
   *
   * @return the g022 Para Uso Reservado da Empresa<br>
   *         Texto de observações destinado para uso exclusivo da Empresa
   */
  public String getReservadoEmpresa() {
    return reservadoEmpresa;
  }

  /**
   * # g022 Para Uso Reservado da Empresa<br>
   * Texto de observações destinado para uso exclusivo da Empresa.
   *
   * @param reservadoEmpresa the new g022 Para Uso Reservado da Empresa<br>
   *          Texto de observações destinado para uso exclusivo da Empresa
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
  // * Enumeração com os diferentes tipos de lotes suportados pelo arquivo.
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
  // * Buffer com o conteúdo do cabeçalho do arquivo. Criado na inicialização da instância.
  // */
  // private StringBuilder buffHeaderArquivo = new StringBuilder();
  //
  // /**
  // * Hash/Lista com os buffers de cada Batch
  // */
  // private LinkedHashMap<TipoLote, DadosLote> lotes = new LinkedHashMap<TipoLote, DadosLote>();
  //
  // // ### Variáveis do cabeçalho do arquivo utilizados em diferentes partes do arquivo
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
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3)); // Código do Banco na Compensação (001-003)
  // header.append("0000"); // Lote de Serviço (004-007)
  // header.append("0"); // Tipo de Registro (008-008)
  // header.append(RUString.repeatString(9, " ")); // Uso Exclusivo FEBRABAN / CNAB (009-017)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", tipoInscricao, 1)); // Tipo de Inscrição da Empresa (018-018)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14)); // Número de Inscrição da Empresa (019-032)
  // header.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20)); // Código do Convênio no Banco (033-052)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5)); // Agência Mantenedora da Conta (053-057)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgencia, 1)); // Dígito Verificador da Agência (058-058)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroConta, 12)); // Número da Conta Corrente (059-070)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvConta, 1)); // Dígito Verificador da Conta (071-071)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgenciaConta, 1)); // Dígito Verificador da Ag/Conta (072-072)
  // header.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30)); // Nome da Empresa (073-102)
  // header.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeBanco, 30)); // Nome do Banco (103-132)
  // header.append(RUString.repeatString(10, " ")); // Uso Exclusivo FEBRABAN / CNAB (133-142)
  // header.append("1"); // Código Remessa/Retorno (143-143)
  // header.append(RFW.getDateTime().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmss"))); // Data de Geração do Arquivo (144-151) + Hora de Geração do Arquivo (152-157)
  // header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroSequencial, 6)); // Número Sequencial do Arquivo (158-163)
  // header.append("103"); // Nº da Versão do Layout do Arquivo (164-166)
  // header.append("00000"); // Densidade de Gravação do Arquivo (167-171)
  // header.append(" "); // Para Uso Reservado do Banco (172-191)
  // header.append(RUString.completeOrTruncateUntilLengthRight(" ", reservadoEmpresa, 20)); // Para Uso Reservado da Empresa (192-211)
  // header.append(RUString.repeatString(29, " ")); // Uso Exclusivo FEBRABAN / CNAB (212-240)
  //
  // if (header.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Arquivo de Lote. A linha não ficou com 240 caracteres.");
  // buffHeaderArquivo.append(header).append("\r\n"); // Adiciona quebra de linha ao final
  // }
  //
  // /**
  // * Adiciona um título de cobrança (boleto) de fornecedor (sem ser serviço) ao arquivo.
  // *
  // * @param barCode Código de Barras do Título
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
  // buff.append(RUString.completeUntilLengthLeft("0", this.codigoBanco, 3)); // Código do Banco (já definido no construtor) - Posição 1-3
  // buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(dadosLote.numeroLote), 4)); // Lote de Serviço (deve ser incrementado a cada lote criado) - Posição 4-7
  // buff.append("3"); // Registro Tipo de Registro 8 8 1
  // buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(++dadosLote.contadorRegistros), 3)); // Nº do Registro Nº Seqüencial do Registro no Lote 9-13 5
  // buff.append("J"); // Segmento Código de Segmento no Reg. Detalhe 14-14 1
  // buff.append("000"); // Tipo Tipo de Movimento 15-15 1 + Código Código da Instrução p/ Movimento 16-17 2
  // buff.append(barCode); // Código Barras Código de Barras 18 61 44
  //
  // }
  //
  // /**
  // * Abre o Lote para lançamento de Tículos de Cobrança para o mesmo banco
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
  // dadosLote.buff.append(RUString.completeUntilLengthLeft("0", this.codigoBanco, 3)); // Código do Banco (já definido no construtor) - Posição 1-3
  // dadosLote.buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(dadosLote.numeroLote), 4)); // Lote de Serviço (deve ser incrementado a cada lote criado) - Posição 4-7
  //
  // // Tipo de Registro (Sempre '1' para Header de Lote) - Posição 8-8
  // // Tipo de Operação - Posição 9-9
  // // Tipo de Serviço - Posição 10-11 / '98' = Pagamentos Diversos
  // // Forma de Lançamento - Posição 12-14 / '30' = Liquidação de Títulos do Próprio Banco
  // // Layout do Lote - Posição 14-16
  // // Uso Exclusivo FEBRABAN / CNAB - Posição 17-17 (Brancos)
  // dadosLote.buff.append("1C9830040 ");
  //
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", tipoInscricao, 1)); // Tipo de Inscrição da Empresa (018-018)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14)); // Número de Inscrição da Empresa (019-032)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20)); // Código do Convênio no Banco (033-052)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5)); // Agência Mantenedora da Conta (053-057)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgencia, 1)); // Dígito Verificador da Agência (058-058)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroConta, 12)); // Número da Conta Corrente (059-070)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvConta, 1)); // Dígito Verificador da Conta (071-071)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgenciaConta, 1)); // Dígito Verificador da Ag/Conta (072-072)
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30)); // Nome da Empresa (073-102)
  // dadosLote.buff.append(" "); // Mensagem 1 - Posição 103-142
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", logradouroEmpresa, 30)); // Logradouro Nome da Rua, Av, Pça, Etc 143-172 30
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", numeroEnderecoEmpresa, 5)); // Número Número do Local 173-177 5
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", complementoEmpresa, 15)); // Complemento Casa, Apto, Sala, Etc 178-192 15
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", cidadeEmpresa, 20)); // Cidade Cidade 193-212 20
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", cepEmpresa, 8)); // CEP CEP 213-217 5 + Complemento CEP Complemento do CEP 218-220 3
  // dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", estadoEmpresa, 2)); // Estado Sigla do Estado 221-222 2
  // dadosLote.buff.append(" "); // CNAB Uso Exclusivo da FEBRABAN/CNAB 223-230 8 + Ocorrências Código das Ocorrências p/ Retorno 231-240 10
  //
  // if (dadosLote.buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Lote de Títulos de Cobrança no Memso Banco. A linha não ficou com 240 caracteres.");
  // dadosLote.buff.append("\r\n"); // Adiciona quebra de linha ao final
  // return dadosLote;
  // }

}
