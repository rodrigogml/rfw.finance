package br.eng.rodrigogml.rfw.finance.cnab240.writer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

import br.eng.rodrigogml.rfw.kernel.RFW;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.preprocess.PreProcess;
import br.eng.rodrigogml.rfw.kernel.utils.RUBills;
import br.eng.rodrigogml.rfw.kernel.utils.RUDV;
import br.eng.rodrigogml.rfw.kernel.utils.RUString;
import br.eng.rodrigogml.rfw.kernel.utils.RUTypes;

/**
 * Description: Esta classe auxilia na escrita de um arquivo de remessa CNAB240.<br>
 * Seus métodos permitem que dados sejam adicionados para a geração rápida do arquivo compatível CNAB240.<br>
 * Por se tratar do sistema financeiro brasileiro esta classe utilizará nomes em português para simplificar a associação aos instrumentos financeiros brasileiros. <Br>
 * <bR>
 * <br>
 * <b>Resumo dos padrões do arquivo:</b>
 * <li>Campos númericos devem sempre estar alinhados a direita e preenchidos com 0 a esquerda, não utilizar espaços.
 * <li>Campos Altanuméricos devem sempre estar alinhados a esuqerda e preenchidos com brancos a direita. <bR>
 * <bR>
 * <b>Arquivo utilizado como Referência:</b> https://cmsarquivos.febraban.org.br/Arquivos/documentos/PDF/Layout%20padrao%20CNAB240%20V%2010%2009%20-%2014_10_21.pdf
 *
 * @author Rodrigo Leitão
 * @since (14 de fev. de 2025)
 */
public class CNAB240 {

  /**
   * Enumeração com os diferentes tipos de lotes suportados pelo arquivo.
   */
  public static enum TipoLote {
    /**
     * Bloco com pagamentos de boletos do mesmo banco.
     */
    TITULODECOBRANCA_MESMOBANCO,
    /**
     * Bloco com pagamentos de boletos de outros bancos.
     */
    TITULODECOBRANCA_OUTROSBANCOS,
  }

  /**
   * Classe para representar um bloco de Lote que será escrito no arquivo.
   */
  class DadosLote {
    TipoLote tipoLote = null;
    /**
     * Número do lote em relação ao arquivo.<br>
     * Este valor é incrementado a cada lote novo criado e colocado na LinkedHashMap, refletindo seu "índice" na hash e posteriormente no arquivo.
     */
    int numeroLote = -1;
    /**
     * Número total de registros dentro do lote.<br>
     * O registro é contato por bloco de informações, um registro pode conter várias linhas, vários segmentos.
     */
    int contadorRegistros = 0;
    /**
     * Número de linhas (segmentos) de um lote. Deve já conter o Header, mas não o Trailer (já que ele nunca é escrito no buffer).<Br>
     * Deve ser incrementado sempre que uma nova linha for escrita dentro do buffer deste objeto.
     */
    int contadorSegmentos = 0;
    /**
     * Acumula o valor dos pagamentos do lote para ser escrito no Trailer
     */
    BigDecimal acumuladorValor = BigDecimal.ZERO;
    /**
     * Buffer que carrega o header do lote e os segmentos criados.
     */
    final StringBuilder buff = new StringBuilder();
  }

  /**
   * Hash/Lista com os buffers de cada Lote.<br>
   * Como os lotes são numerados dentro do arquivo, a medida que cada lote é criado ele é numerado e deve ser mantido na ordem.
   */
  private LinkedHashMap<TipoLote, DadosLote> lotes = new LinkedHashMap<TipoLote, DadosLote>();

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
  private String numeroSequencialArquivo;

  /**
   * G022 Para Uso Reservado da Empresa<br>
   * Texto de observações destinado para uso exclusivo da Empresa.
   */
  private String reservadoEmpresa;

  /**
   * G032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   */
  private String empresaEndLogradouro;

  /**
   * /** G032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   */
  private String empresaEndNumero;

  /**
   * /** G032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   */
  private String empresaEndComplemento;

  /**
   * G033 Cidade<br>
   * Texto referente ao nome do município componente do endereço utilizado para entrega de correspondência.
   */
  private String empresaEndCidade;

  /**
   * G034 CEP<br>
   * Código adotado pela EBCT (Empresa Brasileira de Correios e Telégrafos), para identificação de logradouros.<br>
   * Informar CEP completo com 8 dígitos. Para Ceps que não contenham o sufixo (extensão do CEP), incluir 000 ao final.
   */
  private String empresaEndCEP;

  /**
   * G036 Estado / Unidade da Federação<br>
   * Código do estado, unidade da federação componente do endereço utilizado para entrega de correspondência.
   */
  private String empresaEndUF;

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
  public String writeFileContent() throws RFWException {
    StringBuilder buff = new StringBuilder();

    // Valida atributos que são requeridos no Header e no Trailer previamente para não ter que validar em todos os blocos, já que ambos registros são obrigatórios em todos os arquivos.
    PreProcess.requiredNonNullCritical(codigoBanco, "Você deve definir o atributo Código do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(tipoInscricao, "Você deve definir o atributo Tipo de Inscrição para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(numeroInscricao, "Você deve definir o atributo Número de Inscrição para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(agencia, "Você deve definir o atributo Agência para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(contaNumero, "Você deve definir o atributo Número da Conta para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(nomeEmpresa, "Você deve definir o atributo Nome da Empresa para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(nomeBanco, "Você deve definir o atributo Nome do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(numeroSequencialArquivo, "Você deve definir o atributo Número Sequencial para gerar o arquivo CNAB240.");

    buff.append(writeFileHeader());

    long totalRegistros = 0;
    long totalSegmentos = 0;
    for (DadosLote lote : lotes.values()) {
      buff.append(lote.buff);
      buff.append(writeBatchTrailer(lote));
      totalRegistros += lote.contadorRegistros;
      totalSegmentos += lote.contadorSegmentos;
    }

    buff.append(writeFileTrailer(totalRegistros, totalSegmentos));

    return RUString.removeNonUTF8(RUString.removeAccents(buff.toString())); // Remove acentos e caracteres não UTF8 para aumentar a compatibilidade do arquivo
  }

  private StringBuilder writeFileTrailer(long totalRegistros, long totalSegmentos) throws RFWCriticalException {
    StringBuilder buff = new StringBuilder();
    // Código do Banco na Compensação 1-3 3 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
    // Lote Lote de Serviço 4 7 4 - Num '9999'
    // +Tipo de Registro 8 8 1 - Num '9'
    // +Uso Exclusivo FEBRABAN/CNAB 9 17 9 - Alfa Brancos
    buff.append("99999         ");
    // Quantidade de Lotes do Arquivo 18 23 6 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + totalRegistros, 6));
    // Quantidade de Registros do Arquivo 24 29 6 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + (totalSegmentos + 1), 6)); // Soma o Header e o Trailer sendo criado
    // Qtde. de Contas Concil. Qtde de Contas p/ Conc. (Lotes) 30 35 6 - Num
    // Uso Exclusivo FEBRABAN/CNAB 36 240 205 - Alfa Brancos
    buff.append("000000                                                                                                                                                                                                             ");

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Trailer para o Arquivo de Lote. A linha não ficou com 240 caracteres.");
    buff.append("\r\n"); // Adiciona quebra de linha ao final
    return buff;
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
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroSequencialArquivo, 6));
    // No da Versão do Layout do Arquivo 164-166 3 - Num '103'
    // +Densidade de Gravação do Arquivo 167-171 5 - Num
    // +Para Uso Reservado do Banco 172-191 20 - Alfa
    buff.append("10300000                    ");
    // Para Uso Reservado da Empresa 192 211 20 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", reservadoEmpresa, 20)); // Para Uso Reservado da Empresa (192-211)
    // Uso Exclusivo FEBRABAN / CNAB 212 240 29 - Alfa Brancos
    buff.append("                             "); // Uso Exclusivo FEBRABAN / CNAB (212-240)

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Arquivo de Lote. A linha não ficou com 240 caracteres.");
    buff.append("\r\n"); // Adiciona quebra de linha ao final
    return buff;
  }

  /**
   * Adiciona um pagamento do tipo título de cobrança.<br>
   * Os pagamentos adicionados por este método são:<Br>
   * <li>Boleto co código de barras (Do mesmo banco ou de outros, o módulo gerenciará os lotes automaticamente).
   *
   * @param barCode Código de Barras do Título de Cobrança, composto por 44 dígitos. Nâo aceita a representação numérica.
   * @param dataVencimento Data de Vencimento Nominal<br>
   *          Data de vencimento nominal.
   * @param valorTitulo G042 Valor do Documento (Nominal)<br>
   *          Valor Nominal do documento, expresso em moeda corrente.
   * @param valorDesconto L002 Valor do Desconto + Abatimento<br>
   *          Valor de desconto (bonificação) sobre o valor nominal do documento, somado ao Valor do abatimento concedido pelo Beneficiário, expresso em moeda corrente.
   * @param valorMoraMulta L003 Valor da Mora + Multa<br>
   *          Valor do juros de mora somado ao Valor da multa, expresso em moeda corrente
   * @param dataPagamento P009 Data do Pagamento<br>
   *          Data do pagamento do compromisso.
   * @param valorPagamento P010 Valor do Pagamento<br>
   *          Valor do pagamento, expresso em moeda corrente.
   * @param docID G064 Número do Documento Atribuído pela Empresa (Seu Número)<br>
   *          Número atribuído pela Empresa (Pagador) para identificar o documento de Pagamento (Nota Fiscal, Nota Promissória, etc.)<br>
   *          *Ou um id do pagamento gerado pelo sistema para identificar o pagamento no retorno, tamanho máximo de 20 dígitos.
   * @param beneficiarioNome G013 Nome<Br>
   *          Nome que identifica a pessoa, física ou jurídica, a qual se quer fazer referência.
   * @param beneficiarioTipoInscricao G005 Tipo de Inscrição da Empresa<Br>
   *          Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental.Domínio:
   *          <li>'1' = CPF
   *          <li>'2' = CGC / CNPJ<br>
   *          <b>- Para o Produto/Serviço Cobrança considerar como obrigatório, a partir de 01.06.2015, somente o CPF (código 1) ou o CNPJ (código 2). Os demais códigos não deverão ser utilizados.</b>
   * @param beneficiarioNumeroInscricao G006 Número de Inscrição da Empresa <br>
   *          Número de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental.
   * @throws RFWException
   */
  public void addPayment_TituloCobranca(String barCode, LocalDate dataVencimento, BigDecimal valorTitulo, BigDecimal valorDesconto, BigDecimal valorMoraMulta, LocalDate dataPagamento, BigDecimal valorPagamento, String docID, String beneficiarioNome, String beneficiarioTipoInscricao, String beneficiarioNumeroInscricao) throws RFWException {
    PreProcess.requiredNonNullCritical(codigoBanco, "Você deve definir o atributo Código do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(dataVencimento, "Data de Vencimento não por ser nula.");
    PreProcess.requiredNonNullPositive(valorTitulo, "Deve ser informado um valor válido e positivo para Valor do Título.");
    PreProcess.requiredNonNegative(PreProcess.processBigDecimalToZeroIfNullOrNegative(valorDesconto), "Deve ser informado null ou um valor válido e positivo para Valor do Desconto.");
    PreProcess.requiredNonNegative(PreProcess.processBigDecimalToZeroIfNullOrNegative(valorMoraMulta), "Deve ser informado null ou um valor válido e positivo para Valor da Mora + Multa.");
    PreProcess.requiredNonNullPositive(valorPagamento, "Deve ser informado um valor válido e positivo para Valor do Pagamento.");
    PreProcess.requiredNonNullMatch(docID, "[\\d]{0,20}");
    PreProcess.requiredNonNull(dataPagamento, "Data de pagamento não pode ser nula!");
    RUDV.validateCPFOrCNPJ(beneficiarioNumeroInscricao);

    barCode = barCode.replaceAll("[^\\d]+", "");
    RUBills.isBoletoBarCodeValid(barCode);

    if (!barCode.substring(3, 4).equals("9")) throw new RFWCriticalException("Este método não suporta pagamentos em outra moeda que não Real (Código 9).");

    String bancoBoleto = barCode.substring(0, 3);
    DadosLote lote = null;

    if (bancoBoleto.equals(codigoBanco)) {
      lote = getLoteTituloPagamento(TipoLote.TITULODECOBRANCA_MESMOBANCO);
    } else {
      lote = getLoteTituloPagamento(TipoLote.TITULODECOBRANCA_OUTROSBANCOS);
    }

    // ### SEGMENTO J
    lote.contadorRegistros++;
    StringBuilder buff = new StringBuilder();
    // Código do Banco na Compensação 1-3 3 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
    // Lote de Serviço 4 7 4 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.numeroLote, 4));
    // Tipo de Registro 8 8 1 - Num '3'
    buff.append("3");
    // Nº Seqüencial do Registro no Lote 9 13 5 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.contadorRegistros, 5));
    // Código de Segmento no Reg. Detalhe 14 14 1 - Alfa 'J'
    // +Tipo de Movimento 15 15 1 - Num
    // +...'0' = Indica INCLUSÃO
    // +...'7' = Indica LIQUIDAÇAO
    // +Código da Instrução p/ Movimento 16 17 2 - Num
    // +...'00' = Inclusão de Registro Detalhe Liberado
    buff.append("J700");
    // Código de Barras 18 61 44 - Num
    buff.append(barCode);
    // Nome do Beneficiário 62 91 30 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", beneficiarioNome, 30));
    // Data do Vencimento (Nominal) 92 99 8 - Num
    buff.append(RUTypes.formatToddMMyyyy(dataVencimento));
    // Valor do Título (Nominal) 100 114 13 2 Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", PreProcess.processBigDecimalToZeroIfNullOrNegative(valorTitulo).movePointRight(2).abs().toPlainString(), 15));
    // Valor do Desconto + Abatimento 115 129 13 2 Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", PreProcess.processBigDecimalToZeroIfNullOrNegative(valorDesconto).movePointRight(2).abs().toPlainString(), 15));
    // Valor da Mora + Multa 130 144 13 2 Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", PreProcess.processBigDecimalToZeroIfNullOrNegative(valorMoraMulta).movePointRight(2).abs().toPlainString(), 15));
    // Data do Pagamento 145 152 8 - Num
    buff.append(RUTypes.formatToddMMyyyy(dataPagamento));
    // Valor do Pagamento 153 167 13 2 Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", PreProcess.processBigDecimalToZeroIfNullOrNegative(valorPagamento).movePointRight(2).abs().toPlainString(), 15));
    // Quantidade da Moeda 168 182 10 5 Num [NÃO UTILIZADO NESSE TIPO DE PAGAMENTO]
    buff.append("000000000000000");
    // Nº do Docto Atribuído pela Empresa 183 202 20 - Alfa [NÚMERO DO DOCUMENTO ATRIBUÍDO PELO SISTEMA PRA IDENTIFICAÇÃO NA REMESSA]
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", docID, 20));
    // Nº do Docto Atribuído pelo Banco 203 222 20 - Alfa
    // +Código de Moeda 223 224 2 - Num
    // +...'09' = Real
    // +Uso Exclusivo FEBRABAN/CNAB 225 230 6 - Alfa Brancos
    // +Códigos das Ocorrências p/ Retorno 231 240 10 - Alfa
    buff.append("                    09                ");

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Segmento J para o Lote de Títulos de Cobrança do Mesmo Banco. A linha não ficou com 240 caracteres.");
    lote.buff.append(buff).append("\r\n");
    lote.contadorSegmentos++;
    lote.acumuladorValor = lote.acumuladorValor.add(valorPagamento);
    buff = new StringBuilder();

    // ### SEGMENTO J-52
    // Código do Banco na Compensação 1-3 3 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
    // Lote de Serviço 4 7 4 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.numeroLote, 4));
    // Tipo de Registro 8 8 1 - Num '3'
    buff.append("3");
    // Nº Seqüencial do Registro no Lote 9 13 5 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.contadorRegistros, 5));
    // Código de Segmento no Reg. Detalhe 14 14 1 - Alfa 'J'
    // +Uso Exclusivo FEBRABAN/CNAB 15 15 1 - Alfa Brancos
    // +Código de Movimento Remessa 16 17 2 - Num [PARA PAGAMENTO NENHUM CÓDIGO E DEFINIDO, MANDANDO 0 POR SER NUMÉRICO]
    // +Identificação Registro Opcional 18 19 2 - Num “52”
    buff.append("J 0052");
    // Dados do Pagador Tipo de Inscrição 20 20 1 - Num
    buff.append(tipoInscricao);
    // Dados do Pagador Número de Inscrição 21 35 15 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 15));
    // Dados do Pagador Nome 36 75 40 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 40));
    // Dados do Beneficiário Tipo de Inscrição 76 76 1 - Num
    buff.append(beneficiarioTipoInscricao);
    // Dados do Beneficiário Número de Inscrição 77 91 15 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", beneficiarioNumeroInscricao, 15));
    // Dados do Beneficiário Nome 92 131 40 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", beneficiarioNome, 40));
    // Dados do Beneficiário Tipo de Inscrição 132 132 1 - Num
    buff.append(beneficiarioTipoInscricao);
    // Dados do Beneficiário Número de Inscrição 133 147 15 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", beneficiarioNumeroInscricao, 15));
    // Dados do Beneficiário Nome 148 187 40 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", beneficiarioNome, 40));
    // Uso Exclusivo FEBRABAN/CNAB 188 240 53 - Alfa Brancos
    buff.append("                                                     ");

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Segmento J52 para o Lote de Títulos de Cobrança do Mesmo Banco. A linha não ficou com 240 caracteres.");
    lote.buff.append(buff).append("\r\n");
    lote.contadorSegmentos++;
  }

  private StringBuilder writeBatchTrailer(DadosLote lote) throws RFWException {
    StringBuilder buff = new StringBuilder();
    switch (lote.tipoLote) {
      case TITULODECOBRANCA_MESMOBANCO:
      case TITULODECOBRANCA_OUTROSBANCOS:
        // Código do Banco na Compensação 1-3 3 - Num
        buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
        // Lote de Serviço 4 7 4 - Num
        buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.numeroLote, 4));
        // Tipo de Registro 8 8 1 - Num '3'
        // +Uso Exclusivo FEBRABAN/CNAB 9 17 9 - Alfa Brancos
        buff.append("5         ");
        // Quantidade de Registros do Lote 18-23 6 - Num [CHAMADO DE CONTADOR DE SEGMENTOS NO CÓDIGO] Incluí +1 para o Header e +1 para o próprio Trailer
        buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + (lote.contadorSegmentos + 1), 6)); // Soma o Trailer
        // Somatória dos Valores 24 41 16 2 Num [SOMATÓRIA DO CAMPO VALOR DE PAGAMENTO DOS SEGMENTOS J]
        buff.append(RUString.completeOrTruncateUntilLengthLeft("0", lote.acumuladorValor.movePointRight(2).abs().toPlainString(), 18));
        // Somatória de Quantidade de Moedas 42 59 13 5 Num [DEIXADO EM ZERO POIS NÃO ESCREVEMOS NESSE CAMPO]
        buff.append("000000000000000000");
        // Número Aviso Débito 60 65 6 - Num
        // +Uso Exclusivo FEBRABAN/CNAB 66 230 165 - Alfa Brancos
        // +Códigos das Ocorrências para Retorno 231 240 10 - Alfa
        buff.append("                                                                                                                                                                                     ");

        // Valida o tamanho do Registro
        if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Trailer para o lote '" + lote.tipoLote + "'. A linha não ficou com 240 caracteres.");
        buff.append("\r\n");
        lote.contadorSegmentos++;
        return buff;
      default:
        throw new RFWCriticalException("Trailer para o lote '" + lote.tipoLote + "' ainda não implementado!");
    }
  }

  /**
   * Retorna o lote de lançamento de títulos de cobrança de outros banco. Cria o lote se necessário.
   *
   * @return
   * @throws RFWException
   */
  private DadosLote getLoteTituloPagamento(TipoLote tipoLote) throws RFWException {
    DadosLote lote = lotes.get(tipoLote);
    if (lote == null) {
      lote = new DadosLote();
      lotes.put(tipoLote, lote);
      lote.numeroLote = lotes.size();
      lote.tipoLote = tipoLote;

      PreProcess.requiredNonNullCritical(codigoBanco, "Você deve definir o atributo Código do Banco para gerar o arquivo CNAB240.");

      StringBuilder buff = new StringBuilder();
      // Código do Banco na Compensação 1-3 3 - Num
      buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
      // Lote de Serviço 4 7 4 - Num
      buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.numeroLote, 4));
      // Tipo de Registro 8-8 1 - Num ‘1’
      // +Tipo da Operação 9-9 1 - Alfa 'C'
      // +Tipo do Serviço 10 11 2 - Num
      // +... '98' = Pagamentos Diversos
      buff.append("1C98");
      switch (tipoLote) {
        case TITULODECOBRANCA_MESMOBANCO:
          // Forma Lançamento Forma de Lançamento 12 13 2 - Num
          // ...'30' = Liquidação de Títulos do Próprio Banco
          buff.append("30");
          break;
        case TITULODECOBRANCA_OUTROSBANCOS:
          // Forma Lançamento Forma de Lançamento 12 13 2 - Num
          // ...'30' = Liquidação de Títulos do Próprio Banco
          buff.append("31");
          break;
      }
      // Layout do Lote Nº da Versão do Layout do Lote 14 16 3 - Num '040'
      // +Uso Exclusivo da FEBRABAN/CNAB 17-17 1 - Alfa Brancos
      buff.append("040 ");
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
      buff.append("          ");
      // Nome da Rua, Av, Pça, Etc 143 172 30 - Alfa
      buff.append(RUString.completeOrTruncateUntilLengthRight(" ", empresaEndLogradouro, 30));
      // Número do Local 173 177 5 - Num
      buff.append(RUString.completeOrTruncateUntilLengthLeft("0", empresaEndNumero, 5));
      // Casa, Apto, Sala, Etc 178 192 15 - Alfa
      buff.append(RUString.completeOrTruncateUntilLengthRight(" ", empresaEndComplemento, 15));
      // Cidade 193 212 20 - Alfa
      buff.append(RUString.completeOrTruncateUntilLengthRight(" ", empresaEndCidade, 20));
      // CEP 213 217 5 - Num
      // +Complemento do CEP 218 220 3 - Alfa
      buff.append(RUString.completeOrTruncateUntilLengthRight(" ", empresaEndCEP, 8));
      // Sigla do Estado 221 222 2 - Alfa
      buff.append(RUString.completeOrTruncateUntilLengthRight(" ", empresaEndUF, 2));
      // Uso Exclusivo da FEBRABAN/CNAB 223 230 8 - Alfa Brancos
      // +Código das Ocorrências p/ Retorno 231 240 10 - Alfa
      buff.append("                  ");

      // Valida o tamanho do Registro
      if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Lote de Títulos de Cobrança do Mesmo Banco. A linha não ficou com 240 caracteres.");
      lote.buff.append(buff).append("\r\n");
      lote.contadorSegmentos++;
    }
    return lote;
  }

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
  public String getNumeroSequencialArquivo() {
    return numeroSequencialArquivo;
  }

  /**
   * # g018 Número Seqüencial do Arquivo<br>
   * Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados. <br>
   * Evoluir um número seqüencial a cada header de arquivo.
   *
   * @param numeroSequencialArquivo the new g018 Número Seqüencial do Arquivo<br>
   *          Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados
   */
  public void setNumeroSequencialArquivo(String numeroSequencialArquivo) throws RFWException {
    PreProcess.requiredNonNullMatch(numeroSequencialArquivo, "\\d{1,6}");
    this.numeroSequencialArquivo = numeroSequencialArquivo;
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

  /**
   * # /** G032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   *
   * @return the /** G032 Endereço<Br>
   *         Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência
   */
  public String getEmpresaEndNumero() {
    return empresaEndNumero;
  }

  /**
   * # /** G032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   *
   * @param empresaEndNumero the new /** G032 Endereço<Br>
   *          Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência
   */
  public void setEmpresaEndNumero(String empresaEndNumero) throws RFWException {
    PreProcess.requiredMatch(empresaEndNumero, "[\\d]{1,5}");
    this.empresaEndNumero = empresaEndNumero;
  }

  /**
   * # g032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   *
   * @return the g032 Endereço<Br>
   *         Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência
   */
  public String getEmpresaEndLogradouro() {
    return empresaEndLogradouro;
  }

  /**
   * # g032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   *
   * @param empresaEndLogradouro the new g032 Endereço<Br>
   *          Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência
   */
  public void setEmpresaEndLogradouro(String empresaEndLogradouro) throws RFWException {
    PreProcess.requiredMatch(empresaEndLogradouro, "[\\w ]{0,30}");
    this.empresaEndLogradouro = empresaEndLogradouro;
  }

  /**
   * # /** G032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   *
   * @return the /** G032 Endereço<Br>
   *         Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência
   */
  public String getEmpresaEndComplemento() {
    return empresaEndComplemento;
  }

  /**
   * # /** G032 Endereço<Br>
   * Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência.<br>
   * Utilizado também para endereço de e-mail para entrega eletrônica da informação e para número de celular para envio de mensagem SMS.
   *
   * @param empresaEndComplemento the new /** G032 Endereço<Br>
   *          Texto referente a localização da rua / avenida, número, complemento e bairro utilizado para entrega de correspondência
   */
  public void setEmpresaEndComplemento(String empresaEndComplemento) throws RFWException {
    PreProcess.requiredMatch(empresaEndComplemento, "[\\w ]{0,15}");
    this.empresaEndComplemento = empresaEndComplemento;
  }

  /**
   * # g033 Cidade<br>
   * Texto referente ao nome do município componente do endereço utilizado para entrega de correspondência.
   *
   * @return the g033 Cidade<br>
   *         Texto referente ao nome do município componente do endereço utilizado para entrega de correspondência
   */
  public String getEmpresaEndCidade() {
    return empresaEndCidade;
  }

  /**
   * # g033 Cidade<br>
   * Texto referente ao nome do município componente do endereço utilizado para entrega de correspondência.
   *
   * @param empresaEndCidade the new g033 Cidade<br>
   *          Texto referente ao nome do município componente do endereço utilizado para entrega de correspondência
   */
  public void setEmpresaEndCidade(String empresaEndCidade) throws RFWException {
    PreProcess.requiredMatch(empresaEndCidade, "[\\w ]{0,20}");
    this.empresaEndCidade = empresaEndCidade;
  }

  /**
   * # g034 CEP<br>
   * Código adotado pela EBCT (Empresa Brasileira de Correios e Telégrafos), para identificação de logradouros.<br>
   * Informar CEP completo com 8 dígitos. Para Ceps que não contenham o sufixo (extensão do CEP), incluir 000 ao final.
   *
   * @return the g034 CEP<br>
   *         Código adotado pela EBCT (Empresa Brasileira de Correios e Telégrafos), para identificação de logradouros
   */
  public String getEmpresaEndCEP() {
    return empresaEndCEP;
  }

  /**
   * # g034 CEP<br>
   * Código adotado pela EBCT (Empresa Brasileira de Correios e Telégrafos), para identificação de logradouros.<br>
   * Informar CEP completo com 8 dígitos. Para Ceps que não contenham o sufixo (extensão do CEP), incluir 000 ao final.
   *
   * @param empresaEndCEP the new g034 CEP<br>
   *          Código adotado pela EBCT (Empresa Brasileira de Correios e Telégrafos), para identificação de logradouros
   */
  public void setEmpresaEndCEP(String empresaEndCEP) throws RFWException {
    PreProcess.requiredMatch(empresaEndCEP, "[\\d]{8}");
    this.empresaEndCEP = empresaEndCEP;
  }

  /**
   * # g036 Estado / Unidade da Federação<br>
   * Código do estado, unidade da federação componente do endereço utilizado para entrega de correspondência.
   *
   * @return the g036 Estado / Unidade da Federação<br>
   *         Código do estado, unidade da federação componente do endereço utilizado para entrega de correspondência
   */
  public String getEmpresaEndUF() {
    return empresaEndUF;
  }

  /**
   * # g036 Estado / Unidade da Federação<br>
   * Código do estado, unidade da federação componente do endereço utilizado para entrega de correspondência.
   *
   * @param empresaEndUF the new g036 Estado / Unidade da Federação<br>
   *          Código do estado, unidade da federação componente do endereço utilizado para entrega de correspondência
   */
  public void setEmpresaEndUF(String empresaEndUF) throws RFWException {
    RUDV.validateUF(empresaEndUF);
    this.empresaEndUF = empresaEndUF;
  }
}
