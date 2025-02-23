package br.eng.rodrigogml.rfw.finance.cnab240.parser.data;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;

/**
 * Description: Representa um registro do segmento O.<br>
 *
 * @author Rodrigo Leit�o
 * @since (21 de fev. de 2025)
 */
public class CNAB240RegisterO implements CNAB240RegisterDetail {

  /**
   * C�digo de Barras encontrado
   */
  private String barCode;

  /**
   * Data de Vencimento no arquivo, no formato DDMMAAAA.
   */
  private String dataVencimento;

  /**
   * Data de Pagamento.
   */
  private String dataPagamento;

  /**
   * Valor do Pagamento Final.
   */
  private String valorPagamento;

  /**
   * N�mero do documento atribu�do pelo sistema pra identifica��o na remessa
   */
  private String docID;

  /**
   * N�mero do documento atribut�do pelo banco.
   */
  private String bankDocID;

  /**
   * C�digos de Ocorrencias de retorno
   */
  private String ocorrencias;

  /**
   * Nome da concession�ria ou do org�o p�blico benefici�rio do pagamento.
   */
  private String concessionariaOrgaoPublico;

  /**
   * Cria um novo objeto a partir da do registro.<br>
   * Nos registros compostos mais de uma linha, chamar o m�todo {@link #addLine(String)}.
   *
   * @throws RFWException
   *
   */
  public CNAB240RegisterO(String line) throws RFWException {
    // C�digo de Segmento no Reg. Detalhe 14 14 1 - Alfa 'J'
    if (!"O".equals(line.substring(13, 14))) {
      throw new RFWCriticalException("Este objeto espera registros do segmento O. Registro encontrado: '${0}'.", new String[] { line.substring(13, 14) });
    }

    // C�digo de Barras 18 61 44 - Alfa N001
    this.barCode = line.substring(17, 61);
    // Nome da Concession�ria / �rg�o P�blico 62 91 30 - Alfa G013
    this.concessionariaOrgaoPublico = line.substring(61, 91);
    // Data do Vencimento (Nominal) 92 99 8 - Num G044
    this.dataVencimento = line.substring(91, 99);
    // Data do Pagamento 100 107 8 - Num P009
    this.dataPagamento = line.substring(99, 107);
    // Valor Pagamento Valor do Pagamento 108 122 13 2 Num P004
    this.valorPagamento = line.substring(107, 122);
    // Seu n�mero N� do Docto Atribu�do pela Empresa 123 142 20 Alfa G064
    this.docID = line.substring(122, 142);
    // Nosso N�mero N� do Docto Atribu�do pelo Banco 143 162 20 - Alfa G043
    this.bankDocID = line.substring(142, 162);
    // C�digos das Ocorr�ncias p/ Retorno 231 240 10 - Alfa *G059
    this.ocorrencias = line.substring(230, 240);
  }

  /**
   * # c�digo de Barras encontrado.
   *
   * @return the c�digo de Barras encontrado
   */
  public String getBarCode() {
    return barCode;
  }

  /**
   * # c�digo de Barras encontrado.
   *
   * @param barCode the new c�digo de Barras encontrado
   */
  public void setBarCode(String barCode) {
    this.barCode = barCode;
  }

  /**
   * # data de Vencimento no arquivo, no formato DDMMAAAA.
   *
   * @return the data de Vencimento no arquivo, no formato DDMMAAAA
   */
  public String getDataVencimento() {
    return dataVencimento;
  }

  /**
   * # data de Vencimento no arquivo, no formato DDMMAAAA.
   *
   * @param dataVencimento the new data de Vencimento no arquivo, no formato DDMMAAAA
   */
  public void setDataVencimento(String dataVencimento) {
    this.dataVencimento = dataVencimento;
  }

  /**
   * # data de Pagamento.
   *
   * @return the data de Pagamento
   */
  public String getDataPagamento() {
    return dataPagamento;
  }

  /**
   * # data de Pagamento.
   *
   * @param dataPagamento the new data de Pagamento
   */
  public void setDataPagamento(String dataPagamento) {
    this.dataPagamento = dataPagamento;
  }

  /**
   * # valor do Pagamento Final.
   *
   * @return the valor do Pagamento Final
   */
  public String getValorPagamento() {
    return valorPagamento;
  }

  /**
   * # valor do Pagamento Final.
   *
   * @param valorPagamento the new valor do Pagamento Final
   */
  public void setValorPagamento(String valorPagamento) {
    this.valorPagamento = valorPagamento;
  }

  /**
   * # n�mero do documento atribu�do pelo sistema pra identifica��o na remessa.
   *
   * @return the n�mero do documento atribu�do pelo sistema pra identifica��o na remessa
   */
  public String getDocID() {
    return docID;
  }

  /**
   * Valor do m�todo {@link #getDocID()} transformado em Long.
   *
   * @return the doc I das long
   */
  public Long getDocIDasLong() {
    if (docID == null) return null;
    return Long.parseLong(docID);
  }

  /**
   * # n�mero do documento atribu�do pelo sistema pra identifica��o na remessa.
   *
   * @param docID the new n�mero do documento atribu�do pelo sistema pra identifica��o na remessa
   */
  public void setDocID(String docID) {
    this.docID = docID;
  }

  /**
   * # c�digos de Ocorrencias de retorno.
   *
   * @return the c�digos de Ocorrencias de retorno
   */
  public String getOcorrencias() {
    return ocorrencias;
  }

  /**
   * Recupera o valor do m�todo {@link #getOcorrencias()} e quebra em um SetList com os c�digos de duas em duas letras.
   *
   * @return the c�digos de Ocorrencias de retorno
   */
  public String[] getOcorrenciasAsArray() {
    if (this.ocorrencias == null) return null;
    return ocorrencias.trim().split("(?<=\\G..)");
  }

  /**
   * # c�digos de Ocorrencias de retorno.
   *
   * @param ocorrencias the new c�digos de Ocorrencias de retorno
   */
  public void setOcorrencias(String ocorrencias) {
    this.ocorrencias = ocorrencias;
  }

  /**
   * # n�mero do documento atribut�do pelo banco.
   *
   * @return the n�mero do documento atribut�do pelo banco
   */
  public String getBankDocID() {
    return bankDocID;
  }

  /**
   * # n�mero do documento atribut�do pelo banco.
   *
   * @param bankDocID the new n�mero do documento atribut�do pelo banco
   */
  public void setBankDocID(String bankDocID) {
    this.bankDocID = bankDocID;
  }

  /**
   * # nome da concession�ria ou do org�o p�blico benefici�rio do pagamento.
   *
   * @return the nome da concession�ria ou do org�o p�blico benefici�rio do pagamento
   */
  public String getConcessionariaOrgaoPublico() {
    return concessionariaOrgaoPublico;
  }

  /**
   * # nome da concession�ria ou do org�o p�blico benefici�rio do pagamento.
   *
   * @param concessionariaOrgaoPublico the new nome da concession�ria ou do org�o p�blico benefici�rio do pagamento
   */
  public void setConcessionariaOrgaoPublico(String concessionariaOrgaoPublico) {
    this.concessionariaOrgaoPublico = concessionariaOrgaoPublico;
  }

}
