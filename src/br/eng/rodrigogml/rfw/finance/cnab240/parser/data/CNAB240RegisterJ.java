package br.eng.rodrigogml.rfw.finance.cnab240.parser.data;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;

/**
 * Description: Representa um registro do retirado do arquivo de Lote, pode representar mais de uma linha, quando os registros são compostos por mais de um segmento. Como por exemplo o segmento J, que pode conter o J52 ou J52 para Pix..<br>
 *
 * @author Rodrigo Leitão
 * @since (21 de fev. de 2025)
 */
public class CNAB240RegisterJ implements CNAB240RegisterDetail {

  /**
   * Código de Barras encontrado
   */
  private String barCode;

  /**
   * Data de Vencimento no arquivo, no formato DDMMAAAA.
   */
  private String dataVencimento;

  /**
   * Nome do Beneficiário do Boleto
   */
  private String beneficiarioNome;

  /**
   * Valor do Título
   */
  private String valorTitulo;

  /**
   * Valor do Desconto
   */
  private String valorDesconto;

  /**
   * Valor da Mora + Multa
   */
  private String valorMoraMulta;

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
   * Tipo de Instrição do beneficiário.<br>
   * Extraído do registro J52.
   */
  private String beneficiarioTipoInscricao;

  /**
   * Número de Inscrição do Beneficiário.<br>
   * Extraído do registro J52.
   */
  private String beneficiarioNumeroInscricao;

  /**
   * Cria um novo objeto a partir da do registro.<br>
   * Nos registros compostos mais de uma linha, chamar o método {@link #addLine(String)}.
   *
   * @throws RFWException
   *
   */
  public CNAB240RegisterJ(String line) throws RFWException {
    // Código de Segmento no Reg. Detalhe 14 14 1 - Alfa 'J'
    if (!"J".equals(line.substring(13, 14))) {
      throw new RFWCriticalException("Este objeto espera registros do segmento J. Registro encontrado: '${0}'.", new String[] { line.substring(13, 14) });
    }

    // Código de Barras 18 61 44 - Num
    this.barCode = line.substring(17, 61);
    // Nome do Beneficiário 62 91 30 - Alfa
    this.beneficiarioNome = line.substring(61, 91);
    // Data do Vencimento (Nominal) 92 99 8 - Num
    this.dataVencimento = line.substring(91, 99);
    // Valor do Título (Nominal) 100 114 13 2 Num
    this.valorTitulo = line.substring(99, 114);
    // Valor do Desconto + Abatimento 115 129 13 2 Num
    this.valorDesconto = line.substring(114, 129);
    // Valor da Mora + Multa 130 144 13 2 Num
    this.valorMoraMulta = line.substring(129, 144);
    // Data do Pagamento 145 152 8 - Num
    this.dataPagamento = line.substring(144, 152);
    // Valor do Pagamento 153 167 13 2 Num
    this.valorPagamento = line.substring(152, 167);
    // Nº do Docto Atribuído pela Empresa 183 202 20 - Alfa [NÚMERO DO DOCUMENTO ATRIBUÍDO PELO SISTEMA PRA IDENTIFICAÇÃO NA REMESSA]
    this.docID = line.substring(182, 202);
    // Nº do Docto Atribuído pelo Banco 203 222 20 - Alfa
    this.bankDocID = line.substring(202, 222);
    // Códigos das Ocorrências p/ Retorno 231 240 10 - Alfa
    this.ocorrencias = line.substring(230, 240);
  }

  public void addLine(String line) throws RFWException {
    String newSegmento = line.substring(13, 14);
    if (!"J".equals(newSegmento)) {
      throw new RFWCriticalException("A linha adicional deve ser do mesmo segmento 'J' para ser adicionada ao registro. Segmento da linha adicional: '${0}'.", new String[] { newSegmento });
    }
    // +Identificação Registro Opcional 18 19 2 - Num “52”
    String identificadorRegistro = line.substring(17, 19);
    if (!"52".equals(identificadorRegistro)) {
      throw new RFWCriticalException("Para o segmento J é esperado o itendificador '52' na linha de registro adicional.");
    }
    // Dados do Beneficiário Tipo de Inscrição 76 76 1 - Num
    this.beneficiarioTipoInscricao = line.substring(75, 76);
    // Dados do Beneficiário Número de Inscrição 77 91 15 - Num
    this.beneficiarioNumeroInscricao = line.substring(76, 91);
    // Dados do Beneficiário Nome 92 131 40 - Alfa
    this.beneficiarioNome = line.substring(91, 131); // Já recuperado do registro principal, mas atualizado aqui
  }

  /**
   * # código de Barras encontrado.
   *
   * @return the código de Barras encontrado
   */
  public String getBarCode() {
    return barCode;
  }

  /**
   * # código de Barras encontrado.
   *
   * @param barCode the new código de Barras encontrado
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
   * # nome do Beneficiário do Boleto.
   *
   * @return the nome do Beneficiário do Boleto
   */
  public String getBeneficiarioNome() {
    return beneficiarioNome;
  }

  /**
   * # nome do Beneficiário do Boleto.
   *
   * @param beneficiarioNome the new nome do Beneficiário do Boleto
   */
  public void setBeneficiarioNome(String beneficiarioNome) {
    this.beneficiarioNome = beneficiarioNome;
  }

  /**
   * # valor do Título.
   *
   * @return the valor do Título
   */
  public String getValorTitulo() {
    return valorTitulo;
  }

  /**
   * # valor do Título.
   *
   * @param valorTitulo the new valor do Título
   */
  public void setValorTitulo(String valorTitulo) {
    this.valorTitulo = valorTitulo;
  }

  /**
   * # valor do Desconto.
   *
   * @return the valor do Desconto
   */
  public String getValorDesconto() {
    return valorDesconto;
  }

  /**
   * # valor do Desconto.
   *
   * @param valorDesconto the new valor do Desconto
   */
  public void setValorDesconto(String valorDesconto) {
    this.valorDesconto = valorDesconto;
  }

  /**
   * # valor da Mora + Multa.
   *
   * @return the valor da Mora + Multa
   */
  public String getValorMoraMulta() {
    return valorMoraMulta;
  }

  /**
   * # valor da Mora + Multa.
   *
   * @param valorMoraMulta the new valor da Mora + Multa
   */
  public void setValorMoraMulta(String valorMoraMulta) {
    this.valorMoraMulta = valorMoraMulta;
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
   * # número do documento atribuído pelo sistema pra identificação na remessa.
   *
   * @return the número do documento atribuído pelo sistema pra identificação na remessa
   */
  public String getDocID() {
    return docID;
  }

  /**
   * Valor do método {@link #getDocID()} transformado em Long.
   */
  public Long getDocIDasLong() {
    if (docID == null) return null;
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
   * # tipo de Instrição do beneficiário.<br>
   * Extraído do registro J52.
   *
   * @return the tipo de Instrição do beneficiário
   */
  public String getBeneficiarioTipoInscricao() {
    return beneficiarioTipoInscricao;
  }

  /**
   * # tipo de Instrição do beneficiário.<br>
   * Extraído do registro J52.
   *
   * @param beneficiarioTipoInscricao the new tipo de Instrição do beneficiário
   */
  public void setBeneficiarioTipoInscricao(String beneficiarioTipoInscricao) {
    this.beneficiarioTipoInscricao = beneficiarioTipoInscricao;
  }

  /**
   * # número de Inscrição do Beneficiário.<br>
   * Extraído do registro J52.
   *
   * @return the número de Inscrição do Beneficiário
   */
  public String getBeneficiarioNumeroInscricao() {
    return beneficiarioNumeroInscricao;
  }

  /**
   * # número de Inscrição do Beneficiário.<br>
   * Extraído do registro J52.
   *
   * @param beneficiarioNumeroInscricao the new número de Inscrição do Beneficiário
   */
  public void setBeneficiarioNumeroInscricao(String beneficiarioNumeroInscricao) {
    this.beneficiarioNumeroInscricao = beneficiarioNumeroInscricao;
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

}
