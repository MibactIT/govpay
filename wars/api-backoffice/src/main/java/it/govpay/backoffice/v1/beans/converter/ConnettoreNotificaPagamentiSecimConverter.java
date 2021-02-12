package it.govpay.backoffice.v1.beans.converter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.utils.service.context.ContextThreadLocal;
import org.springframework.security.core.Authentication;

import it.govpay.backoffice.v1.beans.ConnettoreNotificaPagamentiSecim.TipoConnettoreEnum;
import it.govpay.backoffice.v1.beans.Mailserver;
import it.govpay.backoffice.v1.beans.TipoPendenzaProfiloIndex;
import it.govpay.backoffice.v1.controllers.ApplicazioniController;
import it.govpay.bd.BDConfigWrapper;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.core.autorizzazione.AuthorizationManager;
import it.govpay.core.exceptions.NotAuthorizedException;
import it.govpay.model.ConnettoreNotificaPagamenti.Tipo;
import it.govpay.model.ConnettoreNotificaPagamenti.TipoConnettore;
import it.govpay.model.TipoVersamento;

public class ConnettoreNotificaPagamentiSecimConverter {
	
	public static it.govpay.model.ConnettoreNotificaPagamenti getConnettoreDTO(it.govpay.backoffice.v1.beans.ConnettoreNotificaPagamentiSecim connector, Authentication user, Tipo tipo) throws ServiceException,NotAuthorizedException {
		it.govpay.model.ConnettoreNotificaPagamenti connettore = new it.govpay.model.ConnettoreNotificaPagamenti();
		
		connettore.setAbilitato(connector.Abilitato());
		
		if(connector.Abilitato()) {
			connettore.setCodiceIPA(connector.getCodiceIPA()); 
			connettore.setTipoTracciato(tipo.name());
			connettore.setVersioneCsv(connector.getVersioneCsv());
			connettore.setCodiceCliente(connector.getCodiceCliente());
			connettore.setCodiceIstituto(connector.getCodiceIstituto());
			
			boolean appAuthTipiPendenzaAll = false;
			if(connector.getTipiPendenza() != null) {
				List<String> idTipiVersamento = new ArrayList<>();
				
				for (TipoPendenzaProfiloIndex id : connector.getTipiPendenza()) {
					if(id.getIdTipoPendenza().equals(ApplicazioniController.AUTORIZZA_TIPI_PENDENZA_STAR)) {
						appAuthTipiPendenzaAll = true;
					} else{
						idTipiVersamento.add(id.getIdTipoPendenza());
					}
				}
				
				if(appAuthTipiPendenzaAll) {
					List<String> tipiVersamentoAutorizzati = AuthorizationManager.getTipiVersamentoAutorizzati(user);
					
					if(tipiVersamentoAutorizzati == null)
						throw AuthorizationManager.toNotAuthorizedExceptionNessunTipoVersamentoAutorizzato(user);
					
					if(tipiVersamentoAutorizzati.size() > 0) {
						throw AuthorizationManager.toNotAuthorizedException(user, "l'utenza non e' associata a tutti i tipi pendenza, non puo' dunque autorizzare l'applicazione a tutti i tipi pendenza o abilitare l'autodeterminazione dei tipi pendenza");
					}
					
					connettore.setTipiPendenza(new ArrayList<>());				
				} else {
					connettore.setTipiPendenza(idTipiVersamento);
				}
			}
			
			switch (connector.getTipoConnettore()) {
			case EMAIL:
				connettore.setTipoConnettore(TipoConnettore.EMAIL);
				connettore.setEmailIndirizzo(connector.getEmailIndirizzo());
				it.govpay.bd.configurazione.model.MailServer mailServerDTO = null;
				if(connector.getEmailServer() != null) {
					mailServerDTO = new it.govpay.bd.configurazione.model.MailServer();

					mailServerDTO.setHost(connector.getEmailServer().getHost());
					mailServerDTO.setPort(connector.getEmailServer().getPort().intValue());
					mailServerDTO.setUsername(connector.getEmailServer().getUsername());
					mailServerDTO.setPassword(connector.getEmailServer().getPassword());
					mailServerDTO.setFrom(connector.getEmailServer().getFrom());
					mailServerDTO.setConnectionTimeout(connector.getEmailServer().getConnectionTimeout().intValue());
					mailServerDTO.setReadTimeout(connector.getEmailServer().getReadTimeout().intValue());
				}
				connettore.setMailserver(mailServerDTO);
				break;
			case FILESYSTEM:
				connettore.setTipoConnettore(TipoConnettore.FILE_SYSTEM);
				connettore.setFileSystemPath(connector.getFileSystemPath());
				break;
			}
		}
		
		return connettore;
	}

	public static it.govpay.backoffice.v1.beans.ConnettoreNotificaPagamentiSecim toRsModel(it.govpay.model.ConnettoreNotificaPagamenti connettore) throws ServiceException {
		it.govpay.backoffice.v1.beans.ConnettoreNotificaPagamentiSecim rsModel = new it.govpay.backoffice.v1.beans.ConnettoreNotificaPagamentiSecim();
		BDConfigWrapper configWrapper = new BDConfigWrapper(ContextThreadLocal.get().getTransactionId(), true);
		
		rsModel.setAbilitato(connettore.isAbilitato());
		if(connettore.isAbilitato()) {
			rsModel.setCodiceIPA(connettore.getCodiceIPA());
			rsModel.setVersioneCsv(connettore.getVersioneCsv());
			rsModel.setCodiceCliente(connettore.getCodiceCliente());
			rsModel.setCodiceIstituto(connettore.getCodiceIstituto());
			
			switch (connettore.getTipoConnettore()) {
			case EMAIL:
				rsModel.setTipoConnettore(TipoConnettoreEnum.EMAIL);
				rsModel.setEmailIndirizzo(connettore.getEmailIndirizzo());
				Mailserver mailServerRsModel = null;;

				if(connettore.getMailserver() != null) {
					mailServerRsModel = new Mailserver();

					mailServerRsModel.setHost(connettore.getMailserver().getHost());
					mailServerRsModel.setPort(new BigDecimal(connettore.getMailserver().getPort()));
					mailServerRsModel.setUsername(connettore.getMailserver().getUsername());
					mailServerRsModel.setPassword(connettore.getMailserver().getPassword());
					mailServerRsModel.setFrom(connettore.getMailserver().getFrom());
					mailServerRsModel.setConnectionTimeout(new BigDecimal(connettore.getMailserver().getConnectionTimeout()));
					mailServerRsModel.setReadTimeout(new BigDecimal(connettore.getMailserver().getReadTimeout()));
				}
				rsModel.setEmailServer(mailServerRsModel);
				break;
			case FILE_SYSTEM:
				rsModel.setTipoConnettore(TipoConnettoreEnum.FILESYSTEM);
				rsModel.setFileSystemPath(connettore.getFileSystemPath());
				break;
			case WEB_SERVICE:
				break;
			}
			
			List<TipoPendenzaProfiloIndex> idTipiPendenza = null;
			List<String> tipiPendenza = connettore.getTipiPendenza();
			if(tipiPendenza != null) {
				idTipiPendenza = new ArrayList<>();
				if(tipiPendenza.isEmpty()) {
					TipoPendenzaProfiloIndex tPI = new TipoPendenzaProfiloIndex();
					tPI.setIdTipoPendenza(ApplicazioniController.AUTORIZZA_TIPI_PENDENZA_STAR);
					tPI.setDescrizione(ApplicazioniController.AUTORIZZA_TIPI_PENDENZA_STAR_LABEL);
					idTipiPendenza.add(tPI);
				} else {
					for(String codTipoVersamento: tipiPendenza) {
						try {
							TipoPendenzaProfiloIndex tPI = new TipoPendenzaProfiloIndex();
							TipoVersamento tipoVersamento = AnagraficaManager.getTipoVersamento(configWrapper, codTipoVersamento);
							tPI.setIdTipoPendenza(tipoVersamento.getCodTipoVersamento());
							tPI.setDescrizione(tipoVersamento.getDescrizione());
							idTipiPendenza.add(tPI);
						} catch (NotFoundException e) {
						}
					}
				}
			}
			
			rsModel.setTipiPendenza(idTipiPendenza);
		}
		return rsModel;
	}
}
