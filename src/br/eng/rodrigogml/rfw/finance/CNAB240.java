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
 * Seus métodos permitem que dados sejam adicionados para a geração rápida do arquivo compatível CNAB240.<br>
 * Por se tratar do sistema financeiro brasileiro esta classe utilizará nomes em português para simplificar a associação aos instrumentos financeiros brasileiros.
 *
 * @author Rodrigo Leitão
 * @since (14 de fev. de 2025)
 */
public class CNAB240 {

  /**
   * Enumeração com os diferentes tipos de lotes suportados pelo arquivo.
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
   * Buffer com o conteúdo do cabeçalho do arquivo. Criado na inicialização da instância.
   */
  private StringBuilder buffHeaderArquivo = new StringBuilder();

  /**
   * Hash/Lista com os buffers de cada Batch
   */
  private LinkedHashMap<TipoLote, DadosLote> lotes = new LinkedHashMap<TipoLote, DadosLote>();

  // ### Variáveis do cabeçalho do arquivo utilizados em diferentes partes do arquivo
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
   * Construtor da classe CNAB240, responsável por inicializar os dados do Header do arquivo conforme o layout FEBRABAN 240.
   *
   * @param codigoBanco Código do Banco na Compensação. Identifica a instituição bancária responsável pelo processamento da transação.
   *          <ul>
   *          <b>Posição:</b> 001-003 <br>
   *          <b>Formato:</b> Numérico (3 dígitos) <br>
   *          <b>Descrição (G001):</b> Código utilizado para identificação do banco na compensação.
   *          </ul>
   *
   * @param tipoInscricao Tipo de Inscrição da Empresa. Indica se a empresa é uma Pessoa Física ou Jurídica perante uma Instituição Governamental.
   *          <ul>
   *          <b>Posição:</b> 018-018 <br>
   *          <b>Formato:</b> Numérico (1 dígito) <br>
   *          <b>Descrição (G005):</b> Código que identifica o tipo de inscrição da Empresa ou Pessoa Física. Domínio:
   *          </ul>
   *          <ul>
   *          <li>'0' = Isento / Não Informado</li>
   *          <li>'1' = CPF</li>
   *          <li>'2' = CGC / CNPJ</li>
   *          <li>'3' = PIS / PASEP</li>
   *          <li>'9' = Outros</li>
   *          </ul>
   *          <ul>
   *          - Preenchimento obrigatório para DOC e TED (Forma de Lançamento = 03, 41, 43). - Para pagamento ao SIAPE com crédito em conta, o CPF deve ser do 1º titular.
   *          </ul>
   *
   * @param numeroInscricao Número de Inscrição da Empresa. Contém o número do CNPJ ou CPF do titular da conta.
   *          <ul>
   *          <b>Posição:</b> 019-032 <br>
   *          <b>Formato:</b> Numérico (14 dígitos) <br>
   *          <b>Descrição (G006):</b> Deve ser preenchido sem pontos, traços ou barras.
   *          </ul>
   *
   * @param codigoConvenio Código do Convênio no Banco. Código fornecido pelo banco para identificação do cliente.
   *          <ul>
   *          <b>Posição:</b> 033-052 <br>
   *          <b>Formato:</b> Alfanumérico (20 caracteres) <br>
   *          <b>Descrição (G007):</b> Pode ser um número de identificação atribuído pelo banco ao cliente.
   *          </ul>
   *
   * @param agencia Código da Agência Mantenedora da Conta. Número da agência sem o dígito verificador.
   *          <ul>
   *          <b>Posição:</b> 053-057 <br>
   *          <b>Formato:</b> Numérico (5 dígitos) <br>
   *          <b>Descrição (G008):</b> Código da agência bancária onde a conta está registrada.
   *          </ul>
   *
   * @param dvAgencia Dígito Verificador da Agência. Caso a agência não possua DV, preencher com espaço em branco.
   *          <ul>
   *          <b>Posição:</b> 058-058 <br>
   *          <b>Formato:</b> Alfanumérico (1 caractere) <br>
   *          <b>Descrição (G009):</b> Dígito verificador do código da agência bancária.
   *          </ul>
   *
   * @param numeroConta Número da Conta Corrente. Deve conter o número da conta do cliente sem o dígito verificador.
   *          <ul>
   *          <b>Posição:</b> 059-070 <br>
   *          <b>Formato:</b> Numérico (12 dígitos) <br>
   *          <b>Descrição (G010):</b> Número de identificação da conta bancária do cliente.
   *          </ul>
   *
   * @param dvConta Dígito Verificador da Conta. Informar o dígito verificador da conta corrente. Caso o banco não utilize, preencher com espaço.
   *          <ul>
   *          <b>Posição:</b> 071-071 <br>
   *          <b>Formato:</b> Alfanumérico (1 caractere) <br>
   *          <b>Descrição (G011):</b> Dígito verificador do número da conta bancária.
   *          </ul>
   *
   * @param dvAgenciaConta Dígito Verificador da Agência/Conta. Para bancos que utilizam um único DV para a conta e agência. Caso contrário, preencher com espaço.
   *          <ul>
   *          <b>Posição:</b> 072-072 <br>
   *          <b>Formato:</b> Alfanumérico (1 caractere) <br>
   *          <b>Descrição (G012):</b> Usado por bancos que consolidam um único DV para a conta e agência.
   *          </ul>
   *
   * @param nomeEmpresa Nome da Empresa. Deve conter a razão social ou nome do titular da conta.
   *          <ul>
   *          <b>Posição:</b> 073-102 <br>
   *          <b>Formato:</b> Alfanumérico (30 caracteres) <br>
   *          <b>Descrição (G013):</b> Razão social ou nome do titular da conta.
   *          </ul>
   *
   * @param nomeBanco Nome do Banco. Deve conter o nome do banco destinatário do arquivo.
   *          <ul>
   *          <b>Posição:</b> 103-132 <br>
   *          <b>Formato:</b> Alfanumérico (30 caracteres) <br>
   *          <b>Descrição (G014):</b> Nome da instituição bancária responsável.
   *          </ul>
   *
   * @param numeroSequencial Número Sequencial do Arquivo (NSA). Sequencial único do arquivo para controle de remessa/retorno.
   *          <ul>
   *          <b>Posição:</b> 158-163 <br>
   *          <b>Formato:</b> Numérico (6 dígitos) <br>
   *          <b>Descrição (G018):</b> Sequência numérica única por remessa.<br>
   *          Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados.<Br>
   *          Evoluir um número seqüencial a cada header de arquivo
   *          </ul>
   * @param logradouroEmpresa Nome da Rua, Avenida, Praça, etc.
   *          <ul>
   *          <b>Posição:</b> 143-172 <br>
   *          <b>Formato:</b> Alfanumérico (30 caracteres) <br>
   *          <b>Descrição (G032):</b> Deve conter o nome do logradouro sem abreviações excessivas.
   *          </ul>
   *
   * @param numeroEnderecoEmpresa Número do local.
   *          <ul>
   *          <b>Posição:</b> 173-177 <br>
   *          <b>Formato:</b> Numérico (5 dígitos) <br>
   *          <b>Descrição (G032):</b> Deve ser preenchido sem pontos ou traços.
   *          </ul>
   *
   * @param complementoEmpresa Complemento do endereço (Casa, Apto, Sala, etc.).
   *          <ul>
   *          <b>Posição:</b> 178-192 <br>
   *          <b>Formato:</b> Alfanumérico (15 caracteres) <br>
   *          <b>Descrição (G032):</b> Pode conter informações adicionais do endereço.
   *          </ul>
   *
   * @param cidadeEmpresa Nome da Cidade.
   *          <ul>
   *          <b>Posição:</b> 193-212 <br>
   *          <b>Formato:</b> Alfanumérico (20 caracteres) <br>
   *          <b>Descrição (G033):</b> Nome completo da cidade.
   *          </ul>
   *
   * @param cepEmpresa Código de Endereçamento Postal (CEP).
   *          <ul>
   *          <b>Posição:</b> 213-217 <br>
   *          <b>Formato:</b> Numérico (5 dígitos) <br>
   *          <b>Descrição (G034):</b> Deve ser informado sem separadores.
   *          </ul>
   *
   * @param complementoCepEmpresa Complemento do CEP.
   *          <ul>
   *          <b>Posição:</b> 218-220 <br>
   *          <b>Formato:</b> Alfanumérico (3 caracteres) <br>
   *          <b>Descrição (G035):</b> Complemento do CEP, se houver.
   *          </ul>
   *
   * @param estadoEmpresa Sigla do Estado.
   *          <ul>
   *          <b>Posição:</b> 221-222 <br>
   *          <b>Formato:</b> Alfanumérico (2 caracteres) <br>
   *          <b>Descrição (G036):</b> Deve ser uma sigla válida de estado (ex: SP, RJ).
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
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3)); // Código do Banco na Compensação (001-003)
    header.append("0000"); // Lote de Serviço (004-007)
    header.append("0"); // Tipo de Registro (008-008)
    header.append(RUString.repeatString(9, " ")); // Uso Exclusivo FEBRABAN / CNAB (009-017)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", tipoInscricao, 1)); // Tipo de Inscrição da Empresa (018-018)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14)); // Número de Inscrição da Empresa (019-032)
    header.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20)); // Código do Convênio no Banco (033-052)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5)); // Agência Mantenedora da Conta (053-057)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgencia, 1)); // Dígito Verificador da Agência (058-058)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroConta, 12)); // Número da Conta Corrente (059-070)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvConta, 1)); // Dígito Verificador da Conta (071-071)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgenciaConta, 1)); // Dígito Verificador da Ag/Conta (072-072)
    header.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30)); // Nome da Empresa (073-102)
    header.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeBanco, 30)); // Nome do Banco (103-132)
    header.append(RUString.repeatString(10, " ")); // Uso Exclusivo FEBRABAN / CNAB (133-142)
    header.append("1"); // Código Remessa/Retorno (143-143)
    header.append(RFW.getDateTime().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmss"))); // Data de Geração do Arquivo (144-151) + Hora de Geração do Arquivo (152-157)
    header.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroSequencial, 6)); // Número Sequencial do Arquivo (158-163)
    header.append("103"); // Nº da Versão do Layout do Arquivo (164-166)
    header.append("00000"); // Densidade de Gravação do Arquivo (167-171)
    header.append("                    "); // Para Uso Reservado do Banco (172-191)
    header.append(RUString.completeOrTruncateUntilLengthRight(" ", reservadoEmpresa, 20)); // Para Uso Reservado da Empresa (192-211)
    header.append(RUString.repeatString(29, " ")); // Uso Exclusivo FEBRABAN / CNAB (212-240)

    if (header.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Arquivo de Lote. A linha não ficou com 240 caracteres.");
    buffHeaderArquivo.append(header).append("\r\n"); // Adiciona quebra de linha ao final
  }

  /**
   * Adiciona um título de cobrança (boleto) de fornecedor (sem ser serviço) ao arquivo.
   *
   * @param barCode Código de Barras do Título
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
    buff.append(RUString.completeUntilLengthLeft("0", this.codigoBanco, 3)); // Código do Banco (já definido no construtor) - Posição 1-3
    buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(dadosLote.numeroLote), 4)); // Lote de Serviço (deve ser incrementado a cada lote criado) - Posição 4-7
    buff.append("3"); // Registro Tipo de Registro 8 8 1
    buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(++dadosLote.contadorRegistros), 3)); // Nº do Registro Nº Seqüencial do Registro no Lote 9-13 5
    buff.append("J"); // Segmento Código de Segmento no Reg. Detalhe 14-14 1
    buff.append("000"); // Tipo Tipo de Movimento 15-15 1 + Código Código da Instrução p/ Movimento 16-17 2
    buff.append(barCode); // Código Barras Código de Barras 18 61 44

  }

  private DadosLote criarLoteTitulosCobrancaMesmoBanco() throws RFWException {
    DadosLote dadosLote = new DadosLote();
    this.lotes.put(TipoLote.TITULODECOBRANCA_MESMOBANCO, dadosLote);
    dadosLote.tipoLote = TipoLote.TITULODECOBRANCA_MESMOBANCO;
    dadosLote.numeroLote = this.lotes.size();

    dadosLote.buff.append(RUString.completeUntilLengthLeft("0", this.codigoBanco, 3)); // Código do Banco (já definido no construtor) - Posição 1-3
    dadosLote.buff.append(RUString.completeUntilLengthLeft("0", String.valueOf(dadosLote.numeroLote), 4)); // Lote de Serviço (deve ser incrementado a cada lote criado) - Posição 4-7

    // Tipo de Registro (Sempre '1' para Header de Lote) - Posição 8-8
    // Tipo de Operação - Posição 9-9
    // Tipo de Serviço - Posição 10-11 / '98' = Pagamentos Diversos
    // Forma de Lançamento - Posição 12-14 / '30' = Liquidação de Títulos do Próprio Banco
    // Layout do Lote - Posição 14-16
    // Uso Exclusivo FEBRABAN / CNAB - Posição 17-17 (Brancos)
    dadosLote.buff.append("1C9830040 ");

    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", tipoInscricao, 1)); // Tipo de Inscrição da Empresa (018-018)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 14)); // Número de Inscrição da Empresa (019-032)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", codigoConvenio, 20)); // Código do Convênio no Banco (033-052)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", agencia, 5)); // Agência Mantenedora da Conta (053-057)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgencia, 1)); // Dígito Verificador da Agência (058-058)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroConta, 12)); // Número da Conta Corrente (059-070)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvConta, 1)); // Dígito Verificador da Conta (071-071)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthLeft("0", dvAgenciaConta, 1)); // Dígito Verificador da Ag/Conta (072-072)
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 30)); // Nome da Empresa (073-102)
    dadosLote.buff.append("                                        "); // Mensagem 1 - Posição 103-142
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", logradouroEmpresa, 30)); // Logradouro Nome da Rua, Av, Pça, Etc 143-172 30
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", numeroEnderecoEmpresa, 5)); // Número Número do Local 173-177 5
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", complementoEmpresa, 15)); // Complemento Casa, Apto, Sala, Etc 178-192 15
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", cidadeEmpresa, 20)); // Cidade Cidade 193-212 20
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", cepEmpresa, 8)); // CEP CEP 213-217 5 + Complemento CEP Complemento do CEP 218-220 3
    dadosLote.buff.append(RUString.completeOrTruncateUntilLengthRight(" ", estadoEmpresa, 2)); // Estado Sigla do Estado 221-222 2
    dadosLote.buff.append("                  "); // CNAB Uso Exclusivo da FEBRABAN/CNAB 223-230 8 + Ocorrências Código das Ocorrências p/ Retorno 231-240 10

    if (dadosLote.buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Lote de Títulos de Cobrança no Memso Banco. A linha não ficou com 240 caracteres.");
    dadosLote.buff.append("\r\n"); // Adiciona quebra de linha ao final
    return dadosLote;
  }

}
