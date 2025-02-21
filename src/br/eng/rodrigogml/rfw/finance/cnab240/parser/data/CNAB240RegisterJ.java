package br.eng.rodrigogml.rfw.finance.cnab240.parser.data;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;

/**
 * Description: Representa um registro do retirado do arquivo de Lote, pode representar mais de uma linha, quando os registros s�o compostos por mais de um segmento. Como por exemplo o segmento J, que pode conter o J52 ou J52 para Pix..<br>
 *
 * @author Rodrigo Leit�o
 * @since (21 de fev. de 2025)
 */
public class CNAB240RegisterJ implements CNAB240RegisterDetail {

  /**
   * C�digo de Barras encontrado
   */
  private String barCode;

  /**
   * Data de Vencimento no arquivo, no formato DDMMAAAA.
   */
  private String dataVencimento;

  /**
   * Nome do Benefici�rio do Boleto
   */
  private String beneficiarioNome;

  /**
   * Valor do T�tulo
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
   * Tipo de Instri��o do benefici�rio.<br>
   * Extra�do do registro J52.
   */
  private String beneficiarioTipoInscricao;

  /**
   * N�mero de Inscri��o do Benefici�rio.<br>
   * Extra�do do registro J52.
   */
  private String beneficiarioNumeroInscricao;

  /**
   * Cria um novo objeto a partir da do registro.<br>
   * Nos registros compostos mais de uma linha, chamar o m�todo {@link #addLine(String)}.
   *
   * @throws RFWException
   *
   */
  public CNAB240RegisterJ(String line) throws RFWException {
    // C�digo de Segmento no Reg. Detalhe 14 14 1 - Alfa 'J'
    if (!"J".equals(line.substring(13, 14))) {
      throw new RFWCriticalException("Este objeto espera registros do segmento J. Registro encontrado: '${0}'.", new String[] { line.substring(13, 14) });
    }

    // C�digo de Barras 18 61 44 - Num
    this.barCode = line.substring(17, 61);
    // Nome do Benefici�rio 62 91 30 - Alfa
    this.beneficiarioNome = line.substring(61, 91);
    // Data do Vencimento (Nominal) 92 99 8 - Num
    this.dataVencimento = line.substring(91, 99);
    // Valor do T�tulo (Nominal) 100 114 13 2 Num
    this.valorTitulo = line.substring(99, 114);
    // Valor do Desconto + Abatimento 115 129 13 2 Num
    this.valorDesconto = line.substring(114, 129);
    // Valor da Mora + Multa 130 144 13 2 Num
    this.valorMoraMulta = line.substring(129, 144);
    // Data do Pagamento 145 152 8 - Num
    this.dataPagamento = line.substring(144, 152);
    // Valor do Pagamento 153 167 13 2 Num
    this.valorPagamento = line.substring(152, 167);
    // N� do Docto Atribu�do pela Empresa 183 202 20 - Alfa [N�MERO DO DOCUMENTO ATRIBU�DO PELO SISTEMA PRA IDENTIFICA��O NA REMESSA]
    this.docID = line.substring(182, 202);
    // N� do Docto Atribu�do pelo Banco 203 222 20 - Alfa
    this.bankDocID = line.substring(202, 222);
    // C�digos das Ocorr�ncias p/ Retorno 231 240 10 - Alfa
    this.ocorrencias = line.substring(230, 240);
  }

  public void addLine(String line) throws RFWException {
    String newSegmento = line.substring(13, 14);
    if (!"J".equals(newSegmento)) {
      throw new RFWCriticalException("A linha adicional deve ser do mesmo segmento 'J' para ser adicionada ao registro. Segmento da linha adicional: '${0}'.", new String[] { newSegmento });
    }
    // +Identifica��o Registro Opcional 18 19 2 - Num �52�
    String identificadorRegistro = line.substring(17, 19);
    if (!"52".equals(identificadorRegistro)) {
      throw new RFWCriticalException("Para o segmento J � esperado o itendificador '52' na linha de registro adicional.");
    }
    // Dados do Benefici�rio Tipo de Inscri��o 76 76 1 - Num
    this.beneficiarioTipoInscricao = line.substring(75, 76);
    // Dados do Benefici�rio N�mero de Inscri��o 77 91 15 - Num
    this.beneficiarioNumeroInscricao = line.substring(76, 91);
    // Dados do Benefici�rio Nome 92 131 40 - Alfa
    this.beneficiarioNome = line.substring(91, 131); // J� recuperado do registro principal, mas atualizado aqui
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
   * # nome do Benefici�rio do Boleto.
   *
   * @return the nome do Benefici�rio do Boleto
   */
  public String getBeneficiarioNome() {
    return beneficiarioNome;
  }

  /**
   * # nome do Benefici�rio do Boleto.
   *
   * @param beneficiarioNome the new nome do Benefici�rio do Boleto
   */
  public void setBeneficiarioNome(String beneficiarioNome) {
    this.beneficiarioNome = beneficiarioNome;
  }

  /**
   * # valor do T�tulo.
   *
   * @return the valor do T�tulo
   */
  public String getValorTitulo() {
    return valorTitulo;
  }

  /**
   * # valor do T�tulo.
   *
   * @param valorTitulo the new valor do T�tulo
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
   * # n�mero do documento atribu�do pelo sistema pra identifica��o na remessa.
   *
   * @return the n�mero do documento atribu�do pelo sistema pra identifica��o na remessa
   */
  public String getDocID() {
    return docID;
  }

  /**
   * Valor do m�todo {@link #getDocID()} transformado em Long.
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
   * # tipo de Instri��o do benefici�rio.<br>
   * Extra�do do registro J52.
   *
   * @return the tipo de Instri��o do benefici�rio
   */
  public String getBeneficiarioTipoInscricao() {
    return beneficiarioTipoInscricao;
  }

  /**
   * # tipo de Instri��o do benefici�rio.<br>
   * Extra�do do registro J52.
   *
   * @param beneficiarioTipoInscricao the new tipo de Instri��o do benefici�rio
   */
  public void setBeneficiarioTipoInscricao(String beneficiarioTipoInscricao) {
    this.beneficiarioTipoInscricao = beneficiarioTipoInscricao;
  }

  /**
   * # n�mero de Inscri��o do Benefici�rio.<br>
   * Extra�do do registro J52.
   *
   * @return the n�mero de Inscri��o do Benefici�rio
   */
  public String getBeneficiarioNumeroInscricao() {
    return beneficiarioNumeroInscricao;
  }

  /**
   * # n�mero de Inscri��o do Benefici�rio.<br>
   * Extra�do do registro J52.
   *
   * @param beneficiarioNumeroInscricao the new n�mero de Inscri��o do Benefici�rio
   */
  public void setBeneficiarioNumeroInscricao(String beneficiarioNumeroInscricao) {
    this.beneficiarioNumeroInscricao = beneficiarioNumeroInscricao;
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

}
