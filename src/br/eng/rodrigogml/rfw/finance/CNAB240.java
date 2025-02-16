package br.eng.rodrigogml.rfw.finance;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

import br.eng.rodrigogml.rfw.kernel.RFW;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.preprocess.PreProcess;
import br.eng.rodrigogml.rfw.kernel.utils.RUString;

/**
 * Description: Esta classe representa um arquivo CNAB240.<br>
 * Seus m�todos permitem que dados sejam adicionados para a gera��o r�pida do arquivo compat�vel CNAB240.<br>
 * Por se tratar do sistema financeiro brasileiro esta classe utilizar� nomes em portugu�s para simplificar a associa��o aos instrumentos financeiros brasileiros.
 *
 * @author Rodrigo Leit�o
 * @since (14 de fev. de 2025)
 */
public class CNAB240 {

  /**
   * Enumera��o com os diferentes tipos de lotes suportados pelo arquivo.
   */
  private static enum TipoLote {
    /**
     * Bloco com pagamentos de boletos do mesmo banco.
     */
    TITULODECOBRANCA_MESMOBANCO,
    /**
     * Bloco com pagamentos de boletos de outros bancos.
     */
    TITULODECOBRANCA_OUTROBANCO,
  }

  /**
   * Classe para representar um bloco de Batch sendo criado
   */
  class DadosLote {
    TipoLote tipoLote = null;
    int numeroLote = -1;
    int contadorRegistros = 0;
    final StringBuilder buff = new StringBuilder();
  }

  /**
   * Buffer com o conte�do do cabe�alho do arquivo. Criado na inicializa��o da inst�ncia.
   */
  private StringBuilder buffHeaderArquivo = new StringBuilder();

  /**
   * Hash/Lista com os buffers de cada Batch
   */
  private LinkedHashMap<TipoLote, DadosLote> lotes = new LinkedHashMap<TipoLote, DadosLote>();

  // ### Vari�veis do cabe�alho do arquivo utilizados em diferentes partes do arquivo
  private String codigoBanco;
  private String tipoInscricao;
  private String numeroInscricao;
  private String codigoConvenio;
  private String agencia;
  private String dvAgencia;
  private String numeroConta;
  private String dvConta;
  private String dvAgenciaConta;
  private String nomeEmpresa;
  private String logradouroEmpresa;
  private String numeroEnderecoEmpresa;
  private String complementoEmpresa;
  private String cidadeEmpresa;
  private String cepEmpresa;
  private String estadoEmpresa;

  /**
   * Construtor da classe CNAB240, respons�vel por inicializar os dados do Header do arquivo conforme o layout FEBRABAN 240.
   *
   * @param codigoBanco C�digo do Banco na Compensa��o. Identifica a institui��o banc�ria respons�vel pelo processamento da transa��o.
   *          <ul>
   *          <b>Posi��o:</b> 001-003 <br>
   *          <b>Formato:</b> Num�rico (3 d�gitos) <br>
   *          <b>Descri��o (G001):</b> C�digo utilizado para identifica��o do banco na compensa��o.
   *          </ul>
   *
   * @param tipoInscricao Tipo de Inscri��o da Empresa. Indica se a empresa � uma Pessoa F�sica ou Jur�dica perante uma Institui��o Governamental.
   *          <ul>
   *          <b>Posi��o:</b> 018-018 <br>
   *          <b>Formato:</b> Num�rico (1 d�gito) <br>
   *          <b>Descri��o (G005):</b> C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica. Dom�nio:
   *          </ul>
   *          <ul>
   *          <li>'0' = Isento / N�o Informado</li>
   *          <li>'1' = CPF</li>
   *          <li>'2' = CGC / CNPJ</li>
   *          <li>'3' = PIS / PASEP</li>
   *          <li>'9' = Outros</li>
   *          </ul>
   *          <ul>
   *          - Preenchimento obrigat�rio para DOC e TED (Forma de Lan�amento = 03, 41, 43). - Para pagamento ao SIAPE com cr�dito em conta, o CPF deve ser do 1� titular.
   *          </ul>
   *
   * @param numeroInscricao N�mero de Inscri��o da Empresa. Cont�m o n�mero do CNPJ ou CPF do titular da conta.
   *          <ul>
   *          <b>Posi��o:</b> 019-032 <br>
   *          <b>Formato:</b> Num�rico (14 d�gitos) <br>
   *          <b>Descri��o (G006):</b> Deve ser preenchido sem pontos, tra�os ou barras.
   *          </ul>
   *
   * @param codigoConvenio C�digo do Conv�nio no Banco. C�digo fornecido pelo banco para identifica��o do cliente.
   *          <ul>
   *          <b>Posi��o:</b> 033-052 <br>
   *          <b>Formato:</b> Alfanum�rico (20 caracteres) <br>
   *          <b>Descri��o (G007):</b> Pode ser um n�mero de identifica��o atribu�do pelo banco ao cliente.
   *          </ul>
   *
   * @param agencia C�digo da Ag�ncia Mantenedora da Conta. N�mero da ag�ncia sem o d�gito verificador.
   *          <ul>
   *          <b>Posi��o:</b> 053-057 <br>
   *          <b>Formato:</b> Num�rico (5 d�gitos) <br>
   *          <b>Descri��o (G008):</b> C�digo da ag�ncia banc�ria onde a conta est� registrada.
   *          </ul>
   *
   * @param dvAgencia D�gito Verificador da Ag�ncia. Caso a ag�ncia n�o possua DV, preencher com espa�o em branco.
   *          <ul>
   *          <b>Posi��o:</b> 058-058 <br>
   *          <b>Formato:</b> Alfanum�rico (1 caractere) <br>
   *          <b>Descri��o (G009):</b> D�gito verificador do c�digo da ag�ncia banc�ria.
   *          </ul>
   *
   * @param numeroConta N�mero da Conta Corrente. Deve conter o n�mero da conta do cliente sem o d�gito verificador.
   *          <ul>
   *          <b>Posi��o:</b> 059-070 <br>
   *          <b>Formato:</b> Num�rico (12 d�gitos) <br>
   *          <b>Descri��o (G010):</b> N�mero de identifica��o da conta banc�ria do cliente.
   *          </ul>
   *
   * @param dvConta D�gito Verificador da Conta. Informar o d�gito verificador da conta corrente. Caso o banco n�o utilize, preencher com espa�o.
   *          <ul>
   *          <b>Posi��o:</b> 071-071 <br>
   *          <b>Formato:</b> Alfanum�rico (1 caractere) <br>
   *          <b>Descri��o (G011):</b> D�gito verificador do n�mero da conta banc�ria.
   *          </ul>
   *
   * @param dvAgenciaConta D�gito Verificador da Ag�ncia/Conta. Para bancos que utilizam um �nico DV para a conta e ag�ncia. Caso contr�rio, preencher com espa�o.
   *          <ul>
   *          <b>Posi��o:</b> 072-072 <br>
   *          <b>Formato:</b> Alfanum�rico (1 caractere) <br>
   *          <b>Descri��o (G012):</b> Usado por bancos que consolidam um �nico DV para a conta e ag�ncia.
   *          </ul>
   *
   * @param nomeEmpresa Nome da Empresa. Deve conter a raz�o social ou nome do titular da conta.
   *          <ul>
   *          <b>Posi��o:</b> 073-102 <br>
   *          <b>Formato:</b> Alfanum�rico (30 caracteres) <br>
   *          <b>Descri��o (G013):</b> Raz�o social ou nome do titular da conta.
   *          </ul>
   *
   * @param nomeBanco Nome do Banco. Deve conter o nome do banco destinat�rio do arquivo.
   *          <ul>
   *          <b>Posi��o:</b> 103-132 <br>
   *          <b>Formato:</b> Alfanum�rico (30 caracteres) <br>
   *          <b>Descri��o (G014):</b> Nome da institui��o banc�ria respons�vel.
   *          </ul>
   *
   * @param numeroSequencial N�mero Sequencial do Arquivo (NSA). Sequencial �nico do arquivo para controle de remessa/retorno.
   *          <ul>
   *          <b>Posi��o:</b> 158-163 <br>
   *          <b>Formato:</b> Num�rico (6 d�gitos) <br>
   *          <b>Descri��o (G018):</b> Sequ�ncia num�rica �nica por remessa.<br>
   *          N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados.<Br>
   *          Evoluir um n�mero seq�encial a cada header de arquivo
   *          </ul>
   * @param logradouroEmpresa Nome da Rua, Avenida, Pra�a, etc.
   *          <ul>
   *          <b>Posi��o:</b> 143-172 <br>
   *          <b>Formato:</b> Alfanum�rico (30 caracteres) <br>
   *          <b>Descri��o (G032):</b> Deve conter o nome do logradouro sem abrevia��es excessivas.
   *          </ul>
   *
   * @param numeroEnderecoEmpresa N�mero do local.
   *          <ul>
   *          <b>Posi��o:</b> 173-177 <br>
   *          <b>Formato:</b> Num�rico (5 d�gitos) <br>
   *          <b>Descri��o (G032):</b> Deve ser preenchido sem pontos ou tra�os.
   *          </ul>
   *
   * @param complementoEmpresa Complemento do endere�o (Casa, Apto, Sala, etc.).
   *          <ul>
   *          <b>Posi��o:</b> 178-192 <br>
   *          <b>Formato:</b> Alfanum�rico (15 caracteres) <br>
   *          <b>Descri��o (G032):</b> Pode conter informa��es adicionais do endere�o.
   *          </ul>
   *
   * @param cidadeEmpresa Nome da Cidade.
   *          <ul>
   *          <b>Posi��o:</b> 193-212 <br>
   *          <b>Formato:</b> Alfanum�rico (20 caracteres) <br>
   *          <b>Descri��o (G033):</b> Nome completo da cidade.
   *          </ul>
   *
   * @param cepEmpresa C�digo de Endere�amento Postal (CEP).
   *          <ul>
   *          <b>Posi��o:</b> 213-217 <br>
   *          <b>Formato:</b> Num�rico (5 d�gitos) <br>
   *          <b>Descri��o (G034):</b> Deve ser informado sem separadores.
   *          </ul>
   *
   * @param complementoCepEmpresa Complemento do CEP.
   *          <ul>
   *          <b>Posi��o:</b> 218-220 <br>
   *          <b>Formato:</b> Alfanum�rico (3 caracteres) <br>
   *          <b>Descri��o (G035):</b> Complemento do CEP, se houver.
   *          </ul>
   *
   * @param estadoEmpresa Sigla do Estado.
   *          <ul>
   *          <b>Posi��o:</b> 221-222 <br>
   *          <b>Formato:</b> Alfanum�rico (2 caracteres) <br>
   *          <b>Descri��o (G036):</b> Deve ser uma sigla v�lida de estado (ex: SP, RJ).
   *          </ul>
   *          *
   * @throws RFWException
   */
  public CNAB240(String codigoBanco, String tipoInscricao, String numeroInscricao, String codigoConvenio,
      String agencia, String dvAgencia, String numeroConta, String dvConta, String dvAgenciaConta,
      String nomeEmpresa, String nomeBanco, String numeroSequencial, String reservadoEmpresa,
      String logradouroEmpresa, String numeroEnderecoEmpresa, String complementoEmpresa,
      String cidadeEmpresa, String cepEmpresa, String estadoEmpresa) throws RFWException {

    PreProcess.requiredNonNullMatch(codigoBanco, "\\d{3}");
    PreProcess.requiredNonNullMatch(tipoInscricao, "[01239]");
    PreProcess.requiredNonNullMatch(numeroInscricao, "\\d{114}");
    PreProcess.requiredMatch(codigoConvenio, "\\d{0,20}");
    PreProcess.requiredNonNullMatch(agencia, "\\d{1,5}");
    PreProcess.requiredMatch(dvAgencia, "\\d{1}");
    PreProcess.requiredNonNullMatch(numeroConta, "\\d{1,12}");
    PreProcess.requiredNonNullMatch(dvConta, "\\d{1}");
    PreProcess.requiredMatch(dvAgenciaConta, "\\d{1}");
    PreProcess.requiredMatch(nomeEmpresa, "[\\w ]{0,30}");
    PreProcess.requiredMatch(nomeBanco, "[\\w ]{0,30}");
    PreProcess.requiredNonNullMatch(numeroSequencial, "\\d{1,6}");
    PreProcess.requiredMatch(reservadoEmpresa, "[\\w ]{0,30}");
    PreProcess.requiredMatch(logradouroEmpresa, "[\\w ]{0,30}");
    PreProcess.requiredMatch(numeroEnderecoEmpresa, "[\\d]{0,5}");
    PreProcess.requiredMatch(complementoEmpresa, "[\\w ]{0,15}");
    PreProcess.requiredMatch(cidadeEmpresa, "[\\w ]{0,20}");
    PreProcess.requiredMatch(cepEmpresa, "[\\d]{0,8}");
    PreProcess.requiredMatch(estadoEmpresa, "[\\w]{0,2}");

    this.codigoBanco = codigoBanco;
    this.tipoInscricao = tipoInscricao;
    this.numeroInscricao = numeroInscricao;
    this.codigoConvenio = codigoConvenio;
    this.agencia = agencia;
    this.dvAgencia = dvAgencia;
    this.numeroConta = numeroConta;
    this.dvConta = dvConta;
    this.dvAgenciaConta = dvAgenciaConta;
    this.nomeEmpresa = nomeEmpresa;
    // this.numeroSequencial = numeroSequencial;
    // this.reservadoEmpresa = reservadoEmpresa;
    this.logradouroEmpresa = logradouroEmpresa;
    this.numeroEnderecoEmpresa = numeroEnderecoEmpresa;
    this.complementoEmpresa = complementoEmpresa;
    this.cidadeEmpresa = cidadeEmpresa;
    this.cepEmpresa = cepEmpresa;
    this.estadoEmpresa = estadoEmpresa;

    StringBuilder header = new StringBuilder();
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3)); // C�digo do Banco na Compensa��o (001-003)
    header.append("0000"); // Lote de Servi�o (004-007)
    header.append("0"); // Tipo de Registro (008-008)
    header.append(RUString.repeatString(9, " ")); // Uso Exclusivo FEBRABAN / CNAB (009-017)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", tipoInscricao, 1)); // Tipo de Inscri��o da Empresa (018-018)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14)); // N�mero de Inscri��o da Empresa (019-032)
    header.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20)); // C�digo do Conv�nio no Banco (033-052)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5)); // Ag�ncia Mantenedora da Conta (053-057)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgencia, 1)); // D�gito Verificador da Ag�ncia (058-058)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroConta, 12)); // N�mero da Conta Corrente (059-070)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvConta, 1)); // D�gito Verificador da Conta (071-071)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgenciaConta, 1)); // D�gito Verificador da Ag/Conta (072-072)
    header.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30)); // Nome da Empresa (073-102)
    header.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeBanco, 30)); // Nome do Banco (103-132)
    header.append(RUString.repeatString(10, " ")); // Uso Exclusivo FEBRABAN / CNAB (133-142)
    header.append("1"); // C�digo Remessa/Retorno (143-143)
    header.append(RFW.getDateTime().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmss"))); // Data de Gera��o do Arquivo (144-151) + Hora de Gera��o do Arquivo (152-157)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroSequencial, 6)); // N�mero Sequencial do Arquivo (158-163)
    header.append("103"); // N� da Vers�o do Layout do Arquivo (164-166)
    header.append("00000"); // Densidade de Grava��o do Arquivo (167-171)
    header.append("                    "); // Para Uso Reservado do Banco (172-191)
    header.append(RUString.completeOrTruncateUntilLengthRight(" ", reservadoEmpresa, 20)); // Para Uso Reservado da Empresa (192-211)
    header.append(RUString.repeatString(29, " ")); // Uso Exclusivo FEBRABAN / CNAB (212-240)

    if (header.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Arquivo de Lote. A linha n�o ficou com 240 caracteres.");
    buffHeaderArquivo.append(header).append("\r\n"); // Adiciona quebra de linha ao final
  }

  /**
   * Adiciona um t�tulo de cobran�a (boleto) de fornecedor (sem ser servi�o) ao arquivo.
   *
   * @param barCode C�digo de Barras do T�tulo
   * @throws RFWException
   */
  public void incluirTituloCobranca(String barCode) throws RFWException {
    PreProcess.requiredNonNullMatch(barCode, "\\d{44}");

    String bancoEmissor = barCode.substring(0, 3);
    DadosLote dadosLote = null;
    if (bancoEmissor.equals(this.codigoBanco)) {
      dadosLote = this.lotes.get(TipoLote.TITULODECOBRANCA_MESMOBANCO);
      if (dadosLote == null) dadosLote = criarLoteTitulosCobrancaMesmoBanco();
    } else {
      dadosLote = this.lotes.get(TipoLote.TITULODECOBRANCA_OUTROBANCO);
    }

    StringBuilder buff = new StringBuilder();
    buff.append(RUString.completeUntilLengthLeft("0", this.codigoBanco, 3)); // C�digo do Banco (j� definido no construtor) - Posi��o 1-3
    buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(dadosLote.numeroLote), 4)); // Lote de Servi�o (deve ser incrementado a cada lote criado) - Posi��o 4-7
    buff.append("3"); // Registro Tipo de Registro 8 8 1
    buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(++dadosLote.contadorRegistros), 3)); // N� do Registro N� Seq�encial do Registro no Lote 9-13 5
    buff.append("J"); // Segmento C�digo de Segmento no Reg. Detalhe 14-14 1
    buff.append("000"); // Tipo Tipo de Movimento 15-15 1 + C�digo C�digo da Instru��o p/ Movimento 16-17 2
    buff.append(barCode); // C�digo Barras C�digo de Barras 18 61 44

  }

  private DadosLote criarLoteTitulosCobrancaMesmoBanco() throws RFWException {
    DadosLote dadosLote = new DadosLote();
    this.lotes.put(TipoLote.TITULODECOBRANCA_MESMOBANCO, dadosLote);
    dadosLote.tipoLote = TipoLote.TITULODECOBRANCA_MESMOBANCO;
    dadosLote.numeroLote = this.lotes.size();

    dadosLote.buff.append(RUString.completeUntilLengthLeft("0", this.codigoBanco, 3)); // C�digo do Banco (j� definido no construtor) - Posi��o 1-3
    dadosLote.buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(dadosLote.numeroLote), 4)); // Lote de Servi�o (deve ser incrementado a cada lote criado) - Posi��o 4-7

    // Tipo de Registro (Sempre '1' para Header de Lote) - Posi��o 8-8
    // Tipo de Opera��o - Posi��o 9-9
    // Tipo de Servi�o - Posi��o 10-11 / '98' = Pagamentos Diversos
    // Forma de Lan�amento - Posi��o 12-14 / '30' = Liquida��o de T�tulos do Pr�prio Banco
    // Layout do Lote - Posi��o 14-16
    // Uso Exclusivo FEBRABAN / CNAB - Posi��o 17-17 (Brancos)
    dadosLote.buff.append("1C9830040 ");

    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", tipoInscricao, 1)); // Tipo de Inscri��o da Empresa (018-018)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14)); // N�mero de Inscri��o da Empresa (019-032)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20)); // C�digo do Conv�nio no Banco (033-052)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5)); // Ag�ncia Mantenedora da Conta (053-057)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgencia, 1)); // D�gito Verificador da Ag�ncia (058-058)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroConta, 12)); // N�mero da Conta Corrente (059-070)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvConta, 1)); // D�gito Verificador da Conta (071-071)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgenciaConta, 1)); // D�gito Verificador da Ag/Conta (072-072)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30)); // Nome da Empresa (073-102)
    dadosLote.buff.append("                                        "); // Mensagem 1 - Posi��o 103-142
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", logradouroEmpresa, 30)); // Logradouro Nome da Rua, Av, P�a, Etc 143-172 30
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", numeroEnderecoEmpresa, 5)); // N�mero N�mero do Local 173-177 5
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", complementoEmpresa, 15)); // Complemento Casa, Apto, Sala, Etc 178-192 15
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", cidadeEmpresa, 20)); // Cidade Cidade 193-212 20
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", cepEmpresa, 8)); // CEP CEP 213-217 5 + Complemento CEP Complemento do CEP 218-220 3
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", estadoEmpresa, 2)); // Estado Sigla do Estado 221-222 2
    dadosLote.buff.append("                  "); // CNAB Uso Exclusivo da FEBRABAN/CNAB 223-230 8 + Ocorr�ncias C�digo das Ocorr�ncias p/ Retorno 231-240 10

    if (dadosLote.buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Lote de T�tulos de Cobran�a no Memso Banco. A linha n�o ficou com 240 caracteres.");
    dadosLote.buff.append("\r\n"); // Adiciona quebra de linha ao final
    return dadosLote;
  }

}
