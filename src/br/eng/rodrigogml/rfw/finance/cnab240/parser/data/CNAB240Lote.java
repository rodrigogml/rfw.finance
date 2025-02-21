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
    // +... '98' = Pagamentos Diversos
    this.tipoServico = line.substring(9, 11);
    // Forma Lançamento Forma de Lançamento 12 13 2 - Num
    // ...'30' = Liquidação de Títulos do Próprio Banco
    this.formaLancamento = line.substring(11, 13);
    // Layout do Lote Nº da Versão do Layout do Lote 14 16 3 - Num '040'
    this.layout = line.substring(13, 16);

    if (this.layout.equals("040")) {
      if ("98".equals(this.tipoServico) && "30".equals(this.formaLancamento)) {
        this.tipoLote = TipoLote.TITULODECOBRANCA_MESMOBANCO;
      } else if ("98".equals(this.tipoServico) && "31".equals(this.formaLancamento)) {
        this.tipoLote = TipoLote.TITULODECOBRANCA_OUTROSBANCOS;
      } else {
        throw new RFWCriticalException("Tipo de lote desconhecido para 'Tipo de Serviço = ${0}' e 'Forma de Lançamento = ${1}'.", new String[] { this.tipoServico, this.formaLancamento });
      }
    } else {
      throw new RFWCriticalException("Layout de Header de Lote desconhecido: '${0}'.", new String[] { this.layout });
    }
  }

  public TipoLote getTipoLote() {
    return tipoLote;
  }

  public void setTipoLote(TipoLote tipoLote) {
    this.tipoLote = tipoLote;
  }

  public int getNumeroLote() {
    return numeroLote;
  }

  public void setNumeroLote(int numeroLote) {
    this.numeroLote = numeroLote;
  }

  public String getTipoServico() {
    return tipoServico;
  }

  public void setTipoServico(String tipoServico) {
    this.tipoServico = tipoServico;
  }

  public String getFormaLancamento() {
    return formaLancamento;
  }

  public void setFormaLancamento(String formaLancamento) {
    this.formaLancamento = formaLancamento;
  }

  public String getLayout() {
    return layout;
  }

  public void setLayout(String layout) {
    this.layout = layout;
  }

  public List<CNAB240RegisterDetail> getRegisterList() {
    return registerList;
  }

}
