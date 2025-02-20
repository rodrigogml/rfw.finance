package br.eng.rodrigogml.rfw.finance.cnab240.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWValidationException;

/**
 * Description: Esta classe tem a finalidade de converter um arquivo de retorno CNAB240 recebido de outro sistema, validá-lo e transformá-lo em um objeto estruturado para simplificar sua interpretação pelo sistema.<br>
 *
 * @author Rodrigo Leitão
 * @since (20 de fev. de 2025)
 */
public class CNAB240RetornoLeitor {

  /**
   * Instancia um objeto que representa um arquivo de retorno CNAB240.
   *
   * @param retFile InputStream com o conteúdo do arquivo.
   * @throws RFWException
   */
  public CNAB240RetornoLeitor(InputStream retFile) throws RFWException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(retFile))) {
      String line;
      int countLine = 0;
      int lastRegisterType = -1; // Marca o último registro apra saber se o registro atual é esperado

      while ((line = reader.readLine()) != null) {
        if ("".equals(line)) continue;
        countLine++;

        if (line.length() != 240) throw new RFWValidationException("A linha '${0}' não contém 240 caracteres!", new String[] { "" + countLine });

        String tipoRegistro = line.substring(7, 8);

        // Valida se o registro atual é esperado considerando o tipo do registro anterior
        // + Faz o parser da linha e salva os registros, criando um novo objeto para cada lote ou abertura de registro.
        switch (tipoRegistro) {
          case "0": // Header do Arquivo
            if (lastRegisterType != -1) {
              throw new RFWValidationException("Não é esperado um registro do tipo '0' na linha '${0}'.", new String[] { "" + countLine });
            }
            break;
          case "1": // Header do Lote
            break;
          case "3": // Detalhe
            break;
          case "5": // Trailer do Lote
            break;
          case "9": // Trailer do Arquivo
            break;
          default:
            throw new RFWValidationException("Tipo de registro '${0}' desconhecido encontrado na linha'${1}'.", new String[] { tipoRegistro, "" + countLine });
        }

        // Recupera / valida o código do banco
        // if (!"0".equals(tipoRegistro)) {
        // this.codigoBanco = line.substring(0, 3);
        // } else {
        // if (!this.codigoBanco.equals(line.substring(0, 3))) throw new RFWValidationException("O número do banco da linha '${0}' não segue o número do banco da linha de header do arquivo: '${1}'", new String[] { "" + countLine, this.codigoBanco });
        // }

        // switch (tipoRegistro) {
        // case "0": // Header do Arquivo
        // break;
        // case "1": // Header do Lote
        // if (loteAtual != null) {
        // lotes.add(loteAtual);
        // }
        // loteAtual = new CNAB240Lote(line.substring(9, 11).trim());
        // break;
        // case "3": // Detalhe
        // if (loteAtual != null) {
        // loteAtual.adicionarDetalhe(new CNAB240Detalhe(line));
        // }
        // break;
        // case "5": // Trailer do Lote
        // if (loteAtual != null) {
        // lotes.add(loteAtual);
        // loteAtual = null;
        // }
        // break;
        // case "9": // Trailer do Arquivo
        // trailerArquivo = new CNAB240TrailerArquivo(line);
        // break;
        // }

      }
    } catch (IOException e) {
      throw new RFWCriticalException("Falha ao lêr conteúdo do arquivo!", e);
    }
  }

  public String getCodigoBanco() {
    return codigoBanco;
  }

  public void setCodigoBanco(String codigoBanco) {
    this.codigoBanco = codigoBanco;
  }

}
