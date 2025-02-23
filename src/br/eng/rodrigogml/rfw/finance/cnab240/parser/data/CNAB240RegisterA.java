package br.eng.rodrigogml.rfw.finance.cnab240.parser.data;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.utils.RUTypes;

/**
 * Description: Representa um registro do segmento A encontrado no arquivo.<br>
 *
 * @author Rodrigo Leitão
 * @since (21 de fev. de 2025)
 */
public class CNAB240RegisterA implements CNAB240RegisterDetail {

  /**
   * Data de Pagamento.
   */
  private String dataPagamento;

  /**
   * Valor do Pagamento Final.
   */
  private String valorPagamento;

  /**
   * Número do documento atribuído pelo sistema pra identificação na remessa
   */
  private String docID;

  /**
   * Número do documento atributído pelo banco.
   */
  private String bankDocID;

  /**
   * Códigos de Ocorrencias de retorno
   */
  private String ocorrencias;

  /**
   * Número do banco da conta do favorecido.
   */
  private String favorecidoCodigoBanco;

  /**
   * Número da agência da conta do favorecido.
   */
  private String favorecidoAgencia;

  /**
   * Dígito verificador da agência da conta do favorecido, se houver.
   */
  private String favorecidoAgenciaDV;

  /**
   * Número da conta do favorecido.
   */
  private String favorecidoConta;

  /**
   * Dígito verificador da conta do favorecido.
   */
  private String favorecidoContaDV;

  /**
   * Dígito verificador do conjunto agência + conta do favorecido, se houver.
   */
  private String favorecidoAgenciaContaDV;

  /**
   * Nome do favorecido.
   */
  private String favorecidoNome;

  /**
   * Cria um novo objeto a partir da do registro.<br>
   * Nos registros compostos mais de uma linha, chamar o método {@link #addLine(String)}.
   *
   * @throws RFWException
   *
   */
  public CNAB240RegisterA(String line) throws RFWException {
    // Código de Segmento no Reg. Detalhe 14 14 1 - Alfa 'A'
    if (!"A".equals(line.substring(13, 14))) {
      throw new RFWCriticalException("Este objeto espera registros do segmento A. Registro encontrado: '${0}'.", new String[] { line.substring(13, 14) });
    }

    // Banco Código do Banco do Favorecido 21 23 3 - Num P002
    this.favorecidoCodigoBanco = line.substring(20, 23);
    // Ag. Mantenedora da Cta do Favor. 24 28 5 - Num *G008
    this.favorecidoAgencia = line.substring(23, 28);
    // Dígito Verificador da Agência 29 29 1 - Alfa *G009
    this.favorecidoAgenciaDV = line.substring(28, 29);
    // Número da Conta Corrente 30 41 12 - Num *G010
    this.favorecidoConta = line.substring(29, 41);
    // Dígito Verificador da Conta 42 42 1 - Alfa *G011
    this.favorecidoContaDV = line.substring(41, 42);
    // Dígito Verificador da AG/Conta 43 43 1 - Alfa *G012
    this.favorecidoAgenciaContaDV = line.substring(42, 43);
    // Nome do Favorecido 44 73 30 - Alfa G013
    this.favorecidoNome = line.substring(43, 73);

    // Nº do Docum. Atribuído p/ Empresa 74 93 20 - Alfa G064
    this.docID = line.substring(73, 93);
    // Data do Pagamento 94 101 8 - Num P009
    this.dataPagamento = line.substring(93, 101);
    // Valor do Pagamento 120 134 13 2 Num P010
    this.valorPagamento = line.substring(119, 134);
    // Nº do Docum. Atribuído pelo Banco 135 154 20 - Alfa *G043
    this.bankDocID = line.substring(134, 154);
    // Códigos das Ocorrências p/ Retorno 231 240 10 - Alfa *G059
    this.ocorrencias = line.substring(230, 240);
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
   * @return the data de Pagamento
   * @throws RFWException
   */
  public LocalDate getDataPagamentoAsLocalDate() throws RFWException {
    return RUTypes.parseLocalDate(dataPagamento, "ddMMuuuu");
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
   * @return the valor do Pagamento Final
   */
  public BigDecimal getValorPagamentoAsBigDecimal() {
    return new BigDecimal(valorPagamento).movePointLeft(2);
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
   * # número do documento atribuído pelo sistema pra identificação na remessa.
   *
   * @return the número do documento atribuído pelo sistema pra identificação na remessa
   */
  public String getDocID() {
    return docID;
  }

  /**
   * # número do documento atribuído pelo sistema pra identificação na remessa.
   *
   * @return the número do documento atribuído pelo sistema pra identificação na remessa
   */
  public Long getDocIDAsLong() {
    return Long.parseLong(docID);
  }

  /**
   * # número do documento atribuído pelo sistema pra identificação na remessa.
   *
   * @param docID the new número do documento atribuído pelo sistema pra identificação na remessa
   */
  public void setDocID(String docID) {
    this.docID = docID;
  }

  /**
   * # número do documento atributído pelo banco.
   *
   * @return the número do documento atributído pelo banco
   */
  public String getBankDocID() {
    return bankDocID;
  }

  /**
   * # número do documento atributído pelo banco.
   *
   * @param bankDocID the new número do documento atributído pelo banco
   */
  public void setBankDocID(String bankDocID) {
    this.bankDocID = bankDocID;
  }

  /**
   * # códigos de Ocorrencias de retorno.
   *
   * @return the códigos de Ocorrencias de retorno
   */
  public String getOcorrencias() {
    return ocorrencias;
  }

  /**
   * Recupera o valor do método {@link #getOcorrencias()} e quebra em um SetList com os códigos de duas em duas letras.
   *
   * @return the códigos de Ocorrencias de retorno
   */
  public String[] getOcorrenciasAsArray() {
    if (this.ocorrencias == null) return null;
    return ocorrencias.trim().split("(?<=\\G..)");
  }

  /**
   * # códigos de Ocorrencias de retorno.
   *
   * @param ocorrencias the new códigos de Ocorrencias de retorno
   */
  public void setOcorrencias(String ocorrencias) {
    this.ocorrencias = ocorrencias;
  }

  /**
   * # número do banco da conta do favorecido.
   *
   * @return the número do banco da conta do favorecido
   */
  public String getFavorecidoCodigoBanco() {
    return favorecidoCodigoBanco;
  }

  /**
   * # número do banco da conta do favorecido.
   *
   * @param favorecidoCodigoBanco the new número do banco da conta do favorecido
   */
  public void setFavorecidoCodigoBanco(String favorecidoCodigoBanco) {
    this.favorecidoCodigoBanco = favorecidoCodigoBanco;
  }

  /**
   * # número da agência da conta do favorecido.
   *
   * @return the número da agência da conta do favorecido
   */
  public String getFavorecidoAgencia() {
    return favorecidoAgencia;
  }

  /**
   * # número da agência da conta do favorecido.
   *
   * @param favorecidoAgencia the new número da agência da conta do favorecido
   */
  public void setFavorecidoAgencia(String favorecidoAgencia) {
    this.favorecidoAgencia = favorecidoAgencia;
  }

  /**
   * # dígito verificador da agência da conta do favorecido, se houver.
   *
   * @return the dígito verificador da agência da conta do favorecido, se houver
   */
  public String getFavorecidoAgenciaDV() {
    return favorecidoAgenciaDV;
  }

  /**
   * # dígito verificador da agência da conta do favorecido, se houver.
   *
   * @param favorecidoAgenciaDV the new dígito verificador da agência da conta do favorecido, se houver
   */
  public void setFavorecidoAgenciaDV(String favorecidoAgenciaDV) {
    this.favorecidoAgenciaDV = favorecidoAgenciaDV;
  }

  /**
   * # número da conta do favorecido.
   *
   * @return the número da conta do favorecido
   */
  public String getFavorecidoConta() {
    return favorecidoConta;
  }

  /**
   * # número da conta do favorecido.
   *
   * @param favorecidoConta the new número da conta do favorecido
   */
  public void setFavorecidoConta(String favorecidoConta) {
    this.favorecidoConta = favorecidoConta;
  }

  /**
   * # dígito verificador da conta do favorecido.
   *
   * @return the dígito verificador da conta do favorecido
   */
  public String getFavorecidoContaDV() {
    return favorecidoContaDV;
  }

  /**
   * # dígito verificador da conta do favorecido.
   *
   * @param favorecidoContaDV the new dígito verificador da conta do favorecido
   */
  public void setFavorecidoContaDV(String favorecidoContaDV) {
    this.favorecidoContaDV = favorecidoContaDV;
  }

  /**
   * # dígito verificador do conjunto agência + conta do favorecido, se houver.
   *
   * @return the dígito verificador do conjunto agência + conta do favorecido, se houver
   */
  public String getFavorecidoAgenciaContaDV() {
    return favorecidoAgenciaContaDV;
  }

  /**
   * # dígito verificador do conjunto agência + conta do favorecido, se houver.
   *
   * @param favorecidoAgenciaContaDV the new dígito verificador do conjunto agência + conta do favorecido, se houver
   */
  public void setFavorecidoAgenciaContaDV(String favorecidoAgenciaContaDV) {
    this.favorecidoAgenciaContaDV = favorecidoAgenciaContaDV;
  }

  /**
   * # nome do favorecido.
   *
   * @return the nome do favorecido
   */
  public String getFavorecidoNome() {
    return favorecidoNome;
  }

  /**
   * # nome do favorecido.
   *
   * @param favorecidoNome the new nome do favorecido
   */
  public void setFavorecidoNome(String favorecidoNome) {
    this.favorecidoNome = favorecidoNome;
  }

}
