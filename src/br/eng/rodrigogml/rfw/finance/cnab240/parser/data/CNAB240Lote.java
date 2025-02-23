package br.eng.rodrigogml.rfw.finance.cnab240.parser.data;

import java.util.ArrayList;
import java.util.List;

import br.eng.rodrigogml.rfw.finance.cnab240.writer.CNAB240.TipoLote;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;

/**
 * Description: Representa um lote do arquivo CNAB240. Mantém os dados do Header do Lote e contém as estruturas necessárias para armazenas a coleção de registros filhos.
 *
 * @author Rodrigo Leitão
 * @since (21 de fev. de 2025)
 */
public class CNAB240Lote {

  private TipoLote tipoLote = null;

  /**
   * Número do lote em relação ao arquivo.<br>
   * Este valor é incrementado a cada lote novo criado e colocado na LinkedHashMap, refletindo seu "índice" na hash e posteriormente no arquivo.
   */
  private int numeroLote = -1;

  /**
   * Tipo do Serviço de acordo com a tabela do campo.
   */
  private String tipoServico;

  /**
   * Forma de lançamento de acordo com a tabela do campo.
   */
  private String formaLancamento;

  /**
   * Layout do encontrado no registro
   */
  private String layout;

  /**
   * P014 Indicativo de Forma de Pagamento<br>
   * Possibilitar ao Pagador, mediante acordo com o seu Banco de Relacionamento, a forma de pagamento do compromisso.<Br>
   * <li>01 - Débito em Conta Corrente
   * <li>02 – Débito Empréstimo/Financiamento
   * <li>03 – Débito Cartão de Crédito
   */
  private String indicativoFormaPagamento = null;

  private final List<CNAB240RegisterDetail> registerList = new ArrayList<CNAB240RegisterDetail>();

  /**
   * Cria um novo objeto a partir da do lote.<br>
   *
   * @param line
   * @throws RFWCriticalException
   */
  public CNAB240Lote(String line) throws RFWCriticalException {
    // Lote de Serviço 4 7 4 - Num
    this.numeroLote = Integer.parseInt(line.substring(3, 7));
    // Tipo do Serviço 10 11 2 - Num
    // ... '30' = Pagamento Salários
    // ... '98' = Pagamentos Diversos
    this.tipoServico = line.substring(9, 11);
    // Forma Lançamento Forma de Lançamento 12 13 2 - Num
    // ...'01' = Crédito em Conta Corrente/Salário
    // ...'11' = Pagamento de Contas e Tributos com Código de Barras
    // ...'30' = Liquidação de Títulos do Próprio Banco
    // ...'31' = Pagamento de Títulos de Outros Bancos
    this.formaLancamento = line.substring(11, 13);
    // Layout do Lote Nº da Versão do Layout do Lote 14 16 3 - Num '040'
    this.layout = line.substring(13, 16);

    // Define o Tipo de Lote de acordo com a combinação encontrada entre 'Tipo do Serviço' e 'Forma Lançamento'
    if ("98".equals(this.tipoServico)) { // '98' = Pagamentos Diversos
      if ("30".equals(this.formaLancamento)) { // '30' = Liquidação de Títulos do Próprio Banco
        this.tipoLote = TipoLote.TITULODECOBRANCA_MESMOBANCO;
      } else if ("31".equals(this.formaLancamento)) { // '31' = Pagamento de Títulos de Outros Bancos
        this.tipoLote = TipoLote.TITULODECOBRANCA_OUTROSBANCOS;
      } else if ("11".equals(this.formaLancamento)) { // '11' = Pagamento de Contas e Tributos com Código de Barras
        this.tipoLote = TipoLote.SALARIO;
      }
    } else if ("30".equals(this.tipoServico)) { // '30' = Pagamento Salários
      if ("01".equals(this.formaLancamento)) { // '01' = Crédito em Conta Corrente/Salário
        this.tipoLote = TipoLote.SALARIO;
      }
    }

    if (this.tipoLote == null) {
      throw new RFWCriticalException("Tipo de lote desconhecido para 'Tipo de Serviço = ${0}' e 'Forma de Lançamento = ${1}'.", new String[] { this.tipoServico, this.formaLancamento });
    }

    if (this.layout.equals("040")) {
    } else if (this.layout.equals("046")) {
      // Indicativo da Forma de Pagamento do Serviço 223 224 2 - Num P
      this.indicativoFormaPagamento = line.substring(222, 224);
    } else {
      throw new RFWCriticalException("Layout de Header de Lote desconhecido: '${0}'.", new String[] { this.layout });
    }
  }

  /**
   * Gets the tipo lote.
   *
   * @return the tipo lote
   */
  public TipoLote getTipoLote() {
    return tipoLote;
  }

  /**
   * Sets the tipo lote.
   *
   * @param tipoLote the new tipo lote
   */
  public void setTipoLote(TipoLote tipoLote) {
    this.tipoLote = tipoLote;
  }

  /**
   * # número do lote em relação ao arquivo.<br>
   * Este valor é incrementado a cada lote novo criado e colocado na LinkedHashMap, refletindo seu "índice" na hash e posteriormente no arquivo.
   *
   * @return the número do lote em relação ao arquivo
   */
  public int getNumeroLote() {
    return numeroLote;
  }

  /**
   * # número do lote em relação ao arquivo.<br>
   * Este valor é incrementado a cada lote novo criado e colocado na LinkedHashMap, refletindo seu "índice" na hash e posteriormente no arquivo.
   *
   * @param numeroLote the new número do lote em relação ao arquivo
   */
  public void setNumeroLote(int numeroLote) {
    this.numeroLote = numeroLote;
  }

  /**
   * # tipo do Serviço de acordo com a tabela do campo.
   *
   * @return the tipo do Serviço de acordo com a tabela do campo
   */
  public String getTipoServico() {
    return tipoServico;
  }

  /**
   * # tipo do Serviço de acordo com a tabela do campo.
   *
   * @param tipoServico the new tipo do Serviço de acordo com a tabela do campo
   */
  public void setTipoServico(String tipoServico) {
    this.tipoServico = tipoServico;
  }

  /**
   * # forma de lançamento de acordo com a tabela do campo.
   *
   * @return the forma de lançamento de acordo com a tabela do campo
   */
  public String getFormaLancamento() {
    return formaLancamento;
  }

  /**
   * # forma de lançamento de acordo com a tabela do campo.
   *
   * @param formaLancamento the new forma de lançamento de acordo com a tabela do campo
   */
  public void setFormaLancamento(String formaLancamento) {
    this.formaLancamento = formaLancamento;
  }

  /**
   * # layout do encontrado no registro.
   *
   * @return the layout do encontrado no registro
   */
  public String getLayout() {
    return layout;
  }

  /**
   * # layout do encontrado no registro.
   *
   * @param layout the new layout do encontrado no registro
   */
  public void setLayout(String layout) {
    this.layout = layout;
  }

  /**
   * Gets the register list.
   *
   * @return the register list
   */
  public List<CNAB240RegisterDetail> getRegisterList() {
    return registerList;
  }

  /**
   * # p014 Indicativo de Forma de Pagamento<br>
   * Possibilitar ao Pagador, mediante acordo com o seu Banco de Relacionamento, a forma de pagamento do compromisso.<Br>
   * <li>01 - Débito em Conta Corrente
   * <li>02 – Débito Empréstimo/Financiamento
   * <li>03 – Débito Cartão de Crédito.
   *
   * @return the p014 Indicativo de Forma de Pagamento<br>
   *         Possibilitar ao Pagador, mediante acordo com o seu Banco de Relacionamento, a forma de pagamento do compromisso
   */
  public String getIndicativoFormaPagamento() {
    return indicativoFormaPagamento;
  }

  /**
   * # p014 Indicativo de Forma de Pagamento<br>
   * Possibilitar ao Pagador, mediante acordo com o seu Banco de Relacionamento, a forma de pagamento do compromisso.<Br>
   * <li>01 - Débito em Conta Corrente
   * <li>02 – Débito Empréstimo/Financiamento
   * <li>03 – Débito Cartão de Crédito.
   *
   * @param indicativoFormaPagamento the new p014 Indicativo de Forma de Pagamento<br>
   *          Possibilitar ao Pagador, mediante acordo com o seu Banco de Relacionamento, a forma de pagamento do compromisso
   */
  public void setIndicativoFormaPagamento(String indicativoFormaPagamento) {
    this.indicativoFormaPagamento = indicativoFormaPagamento;
  }

}
