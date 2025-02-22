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
 * Seus m�todos permitem que dados sejam adicionados para a gera��o r�pida do arquivo compat�vel CNAB240.<br>
 * Por se tratar do sistema financeiro brasileiro esta classe utilizar� nomes em portugu�s para simplificar a associa��o aos instrumentos financeiros brasileiros. <Br>
 * <bR>
 * <br>
 * <b>Resumo dos padr�es do arquivo:</b>
 * <li>Campos n�mericos devem sempre estar alinhados a direita e preenchidos com 0 a esquerda, n�o utilizar espa�os.
 * <li>Campos Altanum�ricos devem sempre estar alinhados a esuqerda e preenchidos com brancos a direita. <bR>
 * <bR>
 * <b>Arquivo utilizado como Refer�ncia:</b> https://cmsarquivos.febraban.org.br/Arquivos/documentos/PDF/Layout%20padrao%20CNAB240%20V%2010%2009%20-%2014_10_21.pdf
 *
 * @author Rodrigo Leit�o
 * @since (14 de fev. de 2025)
 */
public class CNAB240 {

  /**
   * Enumera��o com os diferentes tipos de lotes suportados pelo arquivo.
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
   * Classe para representar um bloco de Lote que ser� escrito no arquivo.
   */
  class DadosLote {
    TipoLote tipoLote = null;
    /**
     * N�mero do lote em rela��o ao arquivo.<br>
     * Este valor � incrementado a cada lote novo criado e colocado na LinkedHashMap, refletindo seu "�ndice" na hash e posteriormente no arquivo.
     */
    int numeroLote = -1;
    /**
     * N�mero total de registros dentro do lote.<br>
     * O registro � contato por bloco de informa��es, um registro pode conter v�rias linhas, v�rios segmentos.
     */
    int contadorRegistros = 0;
    /**
     * N�mero de linhas (segmentos) de um lote. Deve j� conter o Header, mas n�o o Trailer (j� que ele nunca � escrito no buffer).<Br>
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
   * Como os lotes s�o numerados dentro do arquivo, a medida que cada lote � criado ele � numerado e deve ser mantido na ordem.
   */
  private LinkedHashMap<TipoLote, DadosLote> lotes = new LinkedHashMap<TipoLote, DadosLote>();

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
  private String numeroSequencialArquivo;

  /**
   * G022 Para Uso Reservado da Empresa<br>
   * Texto de observa��es destinado para uso exclusivo da Empresa.
   */
  private String reservadoEmpresa;

  /**
   * G032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   */
  private String empresaEndLogradouro;

  /**
   * /** G032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   */
  private String empresaEndNumero;

  /**
   * /** G032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   */
  private String empresaEndComplemento;

  /**
   * G033 Cidade<br>
   * Texto referente ao nome do munic�pio componente do endere�o utilizado para entrega de correspond�ncia.
   */
  private String empresaEndCidade;

  /**
   * G034 CEP<br>
   * C�digo adotado pela EBCT (Empresa Brasileira de Correios e Tel�grafos), para identifica��o de logradouros.<br>
   * Informar CEP completo com 8 d�gitos. Para Ceps que n�o contenham o sufixo (extens�o do CEP), incluir 000 ao final.
   */
  private String empresaEndCEP;

  /**
   * G036 Estado / Unidade da Federa��o<br>
   * C�digo do estado, unidade da federa��o componente do endere�o utilizado para entrega de correspond�ncia.
   */
  private String empresaEndUF;

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
  public String writeFileContent() throws RFWException {
    StringBuilder buff = new StringBuilder();

    // Valida atributos que s�o requeridos no Header e no Trailer previamente para n�o ter que validar em todos os blocos, j� que ambos registros s�o obrigat�rios em todos os arquivos.
    PreProcess.requiredNonNullCritical(codigoBanco, "Voc� deve definir o atributo C�digo do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(tipoInscricao, "Voc� deve definir o atributo Tipo de Inscri��o para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(numeroInscricao, "Voc� deve definir o atributo N�mero de Inscri��o para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(agencia, "Voc� deve definir o atributo Ag�ncia para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(contaNumero, "Voc� deve definir o atributo N�mero da Conta para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(nomeEmpresa, "Voc� deve definir o atributo Nome da Empresa para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(nomeBanco, "Voc� deve definir o atributo Nome do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(numeroSequencialArquivo, "Voc� deve definir o atributo N�mero Sequencial para gerar o arquivo CNAB240.");

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

    return RUString.removeNonUTF8(RUString.removeAccents(buff.toString())); // Remove acentos e caracteres n�o UTF8 para aumentar a compatibilidade do arquivo
  }

  private StringBuilder writeFileTrailer(long totalRegistros, long totalSegmentos) throws RFWCriticalException {
    StringBuilder buff = new StringBuilder();
    // C�digo do Banco na Compensa��o 1-3 3 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
    // Lote Lote de Servi�o 4 7 4 - Num '9999'
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
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Trailer para o Arquivo de Lote. A linha n�o ficou com 240 caracteres.");
    buff.append("\r\n"); // Adiciona quebra de linha ao final
    return buff;
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
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroSequencialArquivo, 6));
    // No da Vers�o do Layout do Arquivo 164-166 3 - Num '103'
    // +Densidade de Grava��o do Arquivo 167-171 5 - Num
    // +Para Uso Reservado do Banco 172-191 20 - Alfa
    buff.append("10300000                    ");
    // Para Uso Reservado da Empresa 192 211 20 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", reservadoEmpresa, 20)); // Para Uso Reservado da Empresa (192-211)
    // Uso Exclusivo FEBRABAN / CNAB 212 240 29 - Alfa Brancos
    buff.append("                             "); // Uso Exclusivo FEBRABAN / CNAB (212-240)

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Arquivo de Lote. A linha n�o ficou com 240 caracteres.");
    buff.append("\r\n"); // Adiciona quebra de linha ao final
    return buff;
  }

  /**
   * Adiciona um pagamento do tipo t�tulo de cobran�a.<br>
   * Os pagamentos adicionados por este m�todo s�o:<Br>
   * <li>Boleto co c�digo de barras (Do mesmo banco ou de outros, o m�dulo gerenciar� os lotes automaticamente).
   *
   * @param barCode C�digo de Barras do T�tulo de Cobran�a, composto por 44 d�gitos. N�o aceita a representa��o num�rica.
   * @param dataVencimento Data de Vencimento Nominal<br>
   *          Data de vencimento nominal.
   * @param valorTitulo G042 Valor do Documento (Nominal)<br>
   *          Valor Nominal do documento, expresso em moeda corrente.
   * @param valorDesconto L002 Valor do Desconto + Abatimento<br>
   *          Valor de desconto (bonifica��o) sobre o valor nominal do documento, somado ao Valor do abatimento concedido pelo Benefici�rio, expresso em moeda corrente.
   * @param valorMoraMulta L003 Valor da Mora + Multa<br>
   *          Valor do juros de mora somado ao Valor da multa, expresso em moeda corrente
   * @param dataPagamento P009 Data do Pagamento<br>
   *          Data do pagamento do compromisso.
   * @param valorPagamento P010 Valor do Pagamento<br>
   *          Valor do pagamento, expresso em moeda corrente.
   * @param docID G064 N�mero do Documento Atribu�do pela Empresa (Seu N�mero)<br>
   *          N�mero atribu�do pela Empresa (Pagador) para identificar o documento de Pagamento (Nota Fiscal, Nota Promiss�ria, etc.)<br>
   *          *Ou um id do pagamento gerado pelo sistema para identificar o pagamento no retorno, tamanho m�ximo de 20 d�gitos.
   * @param beneficiarioNome G013 Nome<Br>
   *          Nome que identifica a pessoa, f�sica ou jur�dica, a qual se quer fazer refer�ncia.
   * @param beneficiarioTipoInscricao G005 Tipo de Inscri��o da Empresa<Br>
   *          C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental.Dom�nio:
   *          <li>'1' = CPF
   *          <li>'2' = CGC / CNPJ<br>
   *          <b>- Para o Produto/Servi�o Cobran�a considerar como obrigat�rio, a partir de 01.06.2015, somente o CPF (c�digo 1) ou o CNPJ (c�digo 2). Os demais c�digos n�o dever�o ser utilizados.</b>
   * @param beneficiarioNumeroInscricao G006 N�mero de Inscri��o da Empresa <br>
   *          N�mero de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental.
   * @throws RFWException
   */
  public void addPayment_TituloCobranca(String barCode, LocalDate dataVencimento, BigDecimal valorTitulo, BigDecimal valorDesconto, BigDecimal valorMoraMulta, LocalDate dataPagamento, BigDecimal valorPagamento, String docID, String beneficiarioNome, String beneficiarioTipoInscricao, String beneficiarioNumeroInscricao) throws RFWException {
    PreProcess.requiredNonNullCritical(codigoBanco, "Voc� deve definir o atributo C�digo do Banco para gerar o arquivo CNAB240.");
    PreProcess.requiredNonNullCritical(dataVencimento, "Data de Vencimento n�o por ser nula.");
    PreProcess.requiredNonNullPositive(valorTitulo, "Deve ser informado um valor v�lido e positivo para Valor do T�tulo.");
    PreProcess.requiredNonNegative(PreProcess.processBigDecimalToZeroIfNullOrNegative(valorDesconto), "Deve ser informado null ou um valor v�lido e positivo para Valor do Desconto.");
    PreProcess.requiredNonNegative(PreProcess.processBigDecimalToZeroIfNullOrNegative(valorMoraMulta), "Deve ser informado null ou um valor v�lido e positivo para Valor da Mora + Multa.");
    PreProcess.requiredNonNullPositive(valorPagamento, "Deve ser informado um valor v�lido e positivo para Valor do Pagamento.");
    PreProcess.requiredNonNullMatch(docID, "[\\d]{0,20}");
    PreProcess.requiredNonNull(dataPagamento, "Data de pagamento n�o pode ser nula!");
    RUDV.validateCPFOrCNPJ(beneficiarioNumeroInscricao);

    barCode = barCode.replaceAll("[^\\d]+", "");
    RUBills.isBoletoBarCodeValid(barCode);

    if (!barCode.substring(3, 4).equals("9")) throw new RFWCriticalException("Este m�todo n�o suporta pagamentos em outra moeda que n�o Real (C�digo 9).");

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
    // C�digo do Banco na Compensa��o 1-3 3 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
    // Lote de Servi�o 4 7 4 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.numeroLote, 4));
    // Tipo de Registro 8 8 1 - Num '3'
    buff.append("3");
    // N� Seq�encial do Registro no Lote 9 13 5 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.contadorRegistros, 5));
    // C�digo de Segmento no Reg. Detalhe 14 14 1 - Alfa 'J'
    // +Tipo de Movimento 15 15 1 - Num
    // +...'0' = Indica INCLUS�O
    // +...'7' = Indica LIQUIDA�AO
    // +C�digo da Instru��o p/ Movimento 16 17 2 - Num
    // +...'00' = Inclus�o de Registro Detalhe Liberado
    buff.append("J700");
    // C�digo de Barras 18 61 44 - Num
    buff.append(barCode);
    // Nome do Benefici�rio 62 91 30 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", beneficiarioNome, 30));
    // Data do Vencimento (Nominal) 92 99 8 - Num
    buff.append(RUTypes.formatToddMMyyyy(dataVencimento));
    // Valor do T�tulo (Nominal) 100 114 13 2 Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", PreProcess.processBigDecimalToZeroIfNullOrNegative(valorTitulo).movePointRight(2).abs().toPlainString(), 15));
    // Valor do Desconto + Abatimento 115 129 13 2 Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", PreProcess.processBigDecimalToZeroIfNullOrNegative(valorDesconto).movePointRight(2).abs().toPlainString(), 15));
    // Valor da Mora + Multa 130 144 13 2 Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", PreProcess.processBigDecimalToZeroIfNullOrNegative(valorMoraMulta).movePointRight(2).abs().toPlainString(), 15));
    // Data do Pagamento 145 152 8 - Num
    buff.append(RUTypes.formatToddMMyyyy(dataPagamento));
    // Valor do Pagamento 153 167 13 2 Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", PreProcess.processBigDecimalToZeroIfNullOrNegative(valorPagamento).movePointRight(2).abs().toPlainString(), 15));
    // Quantidade da Moeda 168 182 10 5 Num [N�O UTILIZADO NESSE TIPO DE PAGAMENTO]
    buff.append("000000000000000");
    // N� do Docto Atribu�do pela Empresa 183 202 20 - Alfa [N�MERO DO DOCUMENTO ATRIBU�DO PELO SISTEMA PRA IDENTIFICA��O NA REMESSA]
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", docID, 20));
    // N� do Docto Atribu�do pelo Banco 203 222 20 - Alfa
    // +C�digo de Moeda 223 224 2 - Num
    // +...'09' = Real
    // +Uso Exclusivo FEBRABAN/CNAB 225 230 6 - Alfa Brancos
    // +C�digos das Ocorr�ncias p/ Retorno 231 240 10 - Alfa
    buff.append("                    09                ");

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Segmento J para o Lote de T�tulos de Cobran�a do Mesmo Banco. A linha n�o ficou com 240 caracteres.");
    lote.buff.append(buff).append("\r\n");
    lote.contadorSegmentos++;
    lote.acumuladorValor = lote.acumuladorValor.add(valorPagamento);
    buff = new StringBuilder();

    // ### SEGMENTO J-52
    // C�digo do Banco na Compensa��o 1-3 3 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
    // Lote de Servi�o 4 7 4 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.numeroLote, 4));
    // Tipo de Registro 8 8 1 - Num '3'
    buff.append("3");
    // N� Seq�encial do Registro no Lote 9 13 5 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.contadorRegistros, 5));
    // C�digo de Segmento no Reg. Detalhe 14 14 1 - Alfa 'J'
    // +Uso Exclusivo FEBRABAN/CNAB 15 15 1 - Alfa Brancos
    // +C�digo de Movimento Remessa 16 17 2 - Num [PARA PAGAMENTO NENHUM C�DIGO E DEFINIDO, MANDANDO 0 POR SER NUM�RICO]
    // +Identifica��o Registro Opcional 18 19 2 - Num �52�
    buff.append("J 0052");
    // Dados do Pagador Tipo de Inscri��o 20 20 1 - Num
    buff.append(tipoInscricao);
    // Dados do Pagador N�mero de Inscri��o 21 35 15 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", numeroInscricao, 15));
    // Dados do Pagador Nome 36 75 40 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", nomeEmpresa, 40));
    // Dados do Benefici�rio Tipo de Inscri��o 76 76 1 - Num
    buff.append(beneficiarioTipoInscricao);
    // Dados do Benefici�rio N�mero de Inscri��o 77 91 15 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", beneficiarioNumeroInscricao, 15));
    // Dados do Benefici�rio Nome 92 131 40 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", beneficiarioNome, 40));
    // Dados do Benefici�rio Tipo de Inscri��o 132 132 1 - Num
    buff.append(beneficiarioTipoInscricao);
    // Dados do Benefici�rio N�mero de Inscri��o 133 147 15 - Num
    buff.append(RUString.completeOrTruncateUntilLengthLeft("0", beneficiarioNumeroInscricao, 15));
    // Dados do Benefici�rio Nome 148 187 40 - Alfa
    buff.append(RUString.completeOrTruncateUntilLengthRight(" ", beneficiarioNome, 40));
    // Uso Exclusivo FEBRABAN/CNAB 188 240 53 - Alfa Brancos
    buff.append("                                                     ");

    // Valida o tamanho do Registro
    if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Segmento J52 para o Lote de T�tulos de Cobran�a do Mesmo Banco. A linha n�o ficou com 240 caracteres.");
    lote.buff.append(buff).append("\r\n");
    lote.contadorSegmentos++;
  }

  private StringBuilder writeBatchTrailer(DadosLote lote) throws RFWException {
    StringBuilder buff = new StringBuilder();
    switch (lote.tipoLote) {
      case TITULODECOBRANCA_MESMOBANCO:
      case TITULODECOBRANCA_OUTROSBANCOS:
        // C�digo do Banco na Compensa��o 1-3 3 - Num
        buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
        // Lote de Servi�o 4 7 4 - Num
        buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.numeroLote, 4));
        // Tipo de Registro 8 8 1 - Num '3'
        // +Uso Exclusivo FEBRABAN/CNAB 9 17 9 - Alfa Brancos
        buff.append("5         ");
        // Quantidade de Registros do Lote 18-23 6 - Num [CHAMADO DE CONTADOR DE SEGMENTOS NO C�DIGO] Inclu� +1 para o Header e +1 para o pr�prio Trailer
        buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + (lote.contadorSegmentos + 1), 6)); // Soma o Trailer
        // Somat�ria dos Valores 24 41 16 2 Num [SOMAT�RIA DO CAMPO VALOR DE PAGAMENTO DOS SEGMENTOS J]
        buff.append(RUString.completeOrTruncateUntilLengthLeft("0", lote.acumuladorValor.movePointRight(2).abs().toPlainString(), 18));
        // Somat�ria de Quantidade de Moedas 42 59 13 5 Num [DEIXADO EM ZERO POIS N�O ESCREVEMOS NESSE CAMPO]
        buff.append("000000000000000000");
        // N�mero Aviso D�bito 60 65 6 - Num
        // +Uso Exclusivo FEBRABAN/CNAB 66 230 165 - Alfa Brancos
        // +C�digos das Ocorr�ncias para Retorno 231 240 10 - Alfa
        buff.append("                                                                                                                                                                                     ");

        // Valida o tamanho do Registro
        if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Trailer para o lote '" + lote.tipoLote + "'. A linha n�o ficou com 240 caracteres.");
        buff.append("\r\n");
        lote.contadorSegmentos++;
        return buff;
      default:
        throw new RFWCriticalException("Trailer para o lote '" + lote.tipoLote + "' ainda n�o implementado!");
    }
  }

  /**
   * Retorna o lote de lan�amento de t�tulos de cobran�a de outros banco. Cria o lote se necess�rio.
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

      PreProcess.requiredNonNullCritical(codigoBanco, "Voc� deve definir o atributo C�digo do Banco para gerar o arquivo CNAB240.");

      StringBuilder buff = new StringBuilder();
      // C�digo do Banco na Compensa��o 1-3 3 - Num
      buff.append(RUString.completeOrTruncateUntilLengthLeft("0", codigoBanco, 3));
      // Lote de Servi�o 4 7 4 - Num
      buff.append(RUString.completeOrTruncateUntilLengthLeft("0", "" + lote.numeroLote, 4));
      // Tipo de Registro 8-8 1 - Num �1�
      // +Tipo da Opera��o 9-9 1 - Alfa 'C'
      // +Tipo do Servi�o 10 11 2 - Num
      // +... '98' = Pagamentos Diversos
      buff.append("1C98");
      switch (tipoLote) {
        case TITULODECOBRANCA_MESMOBANCO:
          // Forma Lan�amento Forma de Lan�amento 12 13 2 - Num
          // ...'30' = Liquida��o de T�tulos do Pr�prio Banco
          buff.append("30");
          break;
        case TITULODECOBRANCA_OUTROSBANCOS:
          // Forma Lan�amento Forma de Lan�amento 12 13 2 - Num
          // ...'30' = Liquida��o de T�tulos do Pr�prio Banco
          buff.append("31");
          break;
      }
      // Layout do Lote N� da Vers�o do Layout do Lote 14 16 3 - Num '040'
      // +Uso Exclusivo da FEBRABAN/CNAB 17-17 1 - Alfa Brancos
      buff.append("040 ");
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
      buff.append("          ");
      // Nome da Rua, Av, P�a, Etc 143 172 30 - Alfa
      buff.append(RUString.completeOrTruncateUntilLengthRight(" ", empresaEndLogradouro, 30));
      // N�mero do Local 173 177 5 - Num
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
      // +C�digo das Ocorr�ncias p/ Retorno 231 240 10 - Alfa
      buff.append("                  ");

      // Valida o tamanho do Registro
      if (buff.length() != 240) throw new RFWCriticalException("Falha ao criar o Header para o Lote de T�tulos de Cobran�a do Mesmo Banco. A linha n�o ficou com 240 caracteres.");
      lote.buff.append(buff).append("\r\n");
      lote.contadorSegmentos++;
    }
    return lote;
  }

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
  public String getNumeroSequencialArquivo() {
    return numeroSequencialArquivo;
  }

  /**
   * # g018 N�mero Seq�encial do Arquivo<br>
   * N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados. <br>
   * Evoluir um n�mero seq�encial a cada header de arquivo.
   *
   * @param numeroSequencialArquivo the new g018 N�mero Seq�encial do Arquivo<br>
   *          N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados
   */
  public void setNumeroSequencialArquivo(String numeroSequencialArquivo) throws RFWException {
    PreProcess.requiredNonNullMatch(numeroSequencialArquivo, "\\d{1,6}");
    this.numeroSequencialArquivo = numeroSequencialArquivo;
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

  /**
   * # /** G032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   *
   * @return the /** G032 Endere�o<Br>
   *         Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia
   */
  public String getEmpresaEndNumero() {
    return empresaEndNumero;
  }

  /**
   * # /** G032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   *
   * @param empresaEndNumero the new /** G032 Endere�o<Br>
   *          Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia
   */
  public void setEmpresaEndNumero(String empresaEndNumero) throws RFWException {
    PreProcess.requiredMatch(empresaEndNumero, "[\\d]{1,5}");
    this.empresaEndNumero = empresaEndNumero;
  }

  /**
   * # g032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   *
   * @return the g032 Endere�o<Br>
   *         Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia
   */
  public String getEmpresaEndLogradouro() {
    return empresaEndLogradouro;
  }

  /**
   * # g032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   *
   * @param empresaEndLogradouro the new g032 Endere�o<Br>
   *          Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia
   */
  public void setEmpresaEndLogradouro(String empresaEndLogradouro) throws RFWException {
    PreProcess.requiredMatch(empresaEndLogradouro, "[\\w ]{0,30}");
    this.empresaEndLogradouro = empresaEndLogradouro;
  }

  /**
   * # /** G032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   *
   * @return the /** G032 Endere�o<Br>
   *         Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia
   */
  public String getEmpresaEndComplemento() {
    return empresaEndComplemento;
  }

  /**
   * # /** G032 Endere�o<Br>
   * Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia.<br>
   * Utilizado tamb�m para endere�o de e-mail para entrega eletr�nica da informa��o e para n�mero de celular para envio de mensagem SMS.
   *
   * @param empresaEndComplemento the new /** G032 Endere�o<Br>
   *          Texto referente a localiza��o da rua / avenida, n�mero, complemento e bairro utilizado para entrega de correspond�ncia
   */
  public void setEmpresaEndComplemento(String empresaEndComplemento) throws RFWException {
    PreProcess.requiredMatch(empresaEndComplemento, "[\\w ]{0,15}");
    this.empresaEndComplemento = empresaEndComplemento;
  }

  /**
   * # g033 Cidade<br>
   * Texto referente ao nome do munic�pio componente do endere�o utilizado para entrega de correspond�ncia.
   *
   * @return the g033 Cidade<br>
   *         Texto referente ao nome do munic�pio componente do endere�o utilizado para entrega de correspond�ncia
   */
  public String getEmpresaEndCidade() {
    return empresaEndCidade;
  }

  /**
   * # g033 Cidade<br>
   * Texto referente ao nome do munic�pio componente do endere�o utilizado para entrega de correspond�ncia.
   *
   * @param empresaEndCidade the new g033 Cidade<br>
   *          Texto referente ao nome do munic�pio componente do endere�o utilizado para entrega de correspond�ncia
   */
  public void setEmpresaEndCidade(String empresaEndCidade) throws RFWException {
    PreProcess.requiredMatch(empresaEndCidade, "[\\w ]{0,20}");
    this.empresaEndCidade = empresaEndCidade;
  }

  /**
   * # g034 CEP<br>
   * C�digo adotado pela EBCT (Empresa Brasileira de Correios e Tel�grafos), para identifica��o de logradouros.<br>
   * Informar CEP completo com 8 d�gitos. Para Ceps que n�o contenham o sufixo (extens�o do CEP), incluir 000 ao final.
   *
   * @return the g034 CEP<br>
   *         C�digo adotado pela EBCT (Empresa Brasileira de Correios e Tel�grafos), para identifica��o de logradouros
   */
  public String getEmpresaEndCEP() {
    return empresaEndCEP;
  }

  /**
   * # g034 CEP<br>
   * C�digo adotado pela EBCT (Empresa Brasileira de Correios e Tel�grafos), para identifica��o de logradouros.<br>
   * Informar CEP completo com 8 d�gitos. Para Ceps que n�o contenham o sufixo (extens�o do CEP), incluir 000 ao final.
   *
   * @param empresaEndCEP the new g034 CEP<br>
   *          C�digo adotado pela EBCT (Empresa Brasileira de Correios e Tel�grafos), para identifica��o de logradouros
   */
  public void setEmpresaEndCEP(String empresaEndCEP) throws RFWException {
    PreProcess.requiredMatch(empresaEndCEP, "[\\d]{8}");
    this.empresaEndCEP = empresaEndCEP;
  }

  /**
   * # g036 Estado / Unidade da Federa��o<br>
   * C�digo do estado, unidade da federa��o componente do endere�o utilizado para entrega de correspond�ncia.
   *
   * @return the g036 Estado / Unidade da Federa��o<br>
   *         C�digo do estado, unidade da federa��o componente do endere�o utilizado para entrega de correspond�ncia
   */
  public String getEmpresaEndUF() {
    return empresaEndUF;
  }

  /**
   * # g036 Estado / Unidade da Federa��o<br>
   * C�digo do estado, unidade da federa��o componente do endere�o utilizado para entrega de correspond�ncia.
   *
   * @param empresaEndUF the new g036 Estado / Unidade da Federa��o<br>
   *          C�digo do estado, unidade da federa��o componente do endere�o utilizado para entrega de correspond�ncia
   */
  public void setEmpresaEndUF(String empresaEndUF) throws RFWException {
    RUDV.validateUF(empresaEndUF);
    this.empresaEndUF = empresaEndUF;
  }
}
