/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC 
 * http://www.gov4j.it/govpay
 * 
 * Copyright (c) 2014-2017 Link.it srl (http://www.link.it).
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3, as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package it.govpay.core.business;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.utils.LoggerWrapperFactory;
import org.openspcoop2.utils.UtilsException;
import org.openspcoop2.utils.json.ValidationException;
import org.openspcoop2.utils.logger.beans.Property;
import org.openspcoop2.utils.service.context.ContextThreadLocal;
import org.openspcoop2.utils.service.context.IContext;
import org.slf4j.Logger;

import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.model.Applicazione;
import it.govpay.bd.model.Dominio;
import it.govpay.bd.model.NotificaAppIo;
import it.govpay.bd.model.Promemoria;
import it.govpay.bd.model.TipoVersamentoDominio;
import it.govpay.bd.pagamento.NotificheAppIoBD;
import it.govpay.bd.pagamento.PromemoriaBD;
import it.govpay.bd.pagamento.VersamentiBD;
import it.govpay.core.beans.EsitoOperazione;
import it.govpay.core.business.model.AnnullaVersamentoDTO;
import it.govpay.core.dao.pagamenti.dto.PagamentiPortaleDTO.RefVersamentoAvviso;
import it.govpay.core.dao.pagamenti.dto.PagamentiPortaleDTO.RefVersamentoPendenza;
import it.govpay.core.exceptions.GovPayException;
import it.govpay.core.exceptions.NotAuthorizedException;
import it.govpay.core.exceptions.VersamentoAnnullatoException;
import it.govpay.core.exceptions.VersamentoDuplicatoException;
import it.govpay.core.exceptions.VersamentoNonValidoException;
import it.govpay.core.exceptions.VersamentoScadutoException;
import it.govpay.core.exceptions.VersamentoSconosciutoException;
import it.govpay.core.utils.AvvisaturaUtils;
import it.govpay.core.utils.GovpayConfig;
import it.govpay.core.utils.GpContext;
import it.govpay.core.utils.IuvUtils;
import it.govpay.core.utils.VersamentoUtils;
import it.govpay.core.utils.client.BasicClient.ClientException;
import it.govpay.model.Iuv.TipoIUV;
import it.govpay.model.NotificaAppIo.TipoNotifica;
import it.govpay.model.Versamento.AvvisaturaOperazione;
import it.govpay.model.Versamento.ModoAvvisatura;
import it.govpay.model.Versamento.StatoPagamento;
import it.govpay.model.Versamento.StatoVersamento;
import it.govpay.model.Versamento.TipologiaTipoVersamento;

public class Versamento extends BasicBD {

	private static final String ECCEZIONE_NON_SPECIFICATA = "- Non specificata -";
	private static final String LOG_KEY_VERSAMENTO_ANNULLA_KO = "versamento.annullaKo";
	private static Logger log = LoggerWrapperFactory.getLogger(Versamento.class);
	
	public Versamento(BasicBD basicBD) {
		super(basicBD);
	}
	
	@Deprecated
	public it.govpay.bd.model.Versamento caricaVersamento(it.govpay.bd.model.Versamento versamento, boolean generaIuv, boolean aggiornaSeEsiste, Boolean avvisatura, Date dataAvvisatura) throws GovPayException {
		// Indica se devo gestire la transazione oppure se e' gestita dal chiamante
		boolean doCommit = false;
		IContext ctx = ContextThreadLocal.get();
		GpContext appContext = (GpContext) ctx.getApplicationContext();

		try {
			ctx.getApplicationLogger().log("versamento.validazioneSemantica", versamento.getApplicazione(this).getCodApplicazione(), versamento.getCodVersamentoEnte());
			VersamentoUtils.validazioneSemantica(versamento, generaIuv, this);
			ctx.getApplicationLogger().log("versamento.validazioneSemanticaOk", versamento.getApplicazione(this).getCodApplicazione(), versamento.getCodVersamentoEnte());
			
			VersamentiBD versamentiBD = new VersamentiBD(this);
			
			appContext.getPagamentoCtx().loadVersamentoContext(versamento, this);
			
			try {
				it.govpay.bd.model.Versamento versamentoLetto = versamentiBD.getVersamento(versamento.getIdApplicazione(), versamento.getCodVersamentoEnte());
				
				if(!aggiornaSeEsiste)
					throw new GovPayException(EsitoOperazione.VER_015, versamento.getApplicazione(this).getCodApplicazione(), versamento.getCodVersamentoEnte());
				
				// Versamento presente.
				versamento.setCreated(false);
				this.copiaPropertiesNonModificabiliVersamento(versamento, versamentoLetto);
				
				// se non erano stati assegnati o proposti iuv e numero avviso e ne e' richiesta l'assegnazione, li assegnio
				if(versamento.getIuvVersamento() == null && generaIuv) {
					Iuv iuvBusiness = new Iuv(this);
					String iuv = iuvBusiness.generaIUV(versamento.getApplicazione(this), versamento.getUo(this).getDominio(this), versamento.getCodVersamentoEnte(), TipoIUV.NUMERICO);
					// imposto iuv calcolato
					versamento.setIuvVersamento(iuv);
					// calcolo il numero avviso
					it.govpay.core.business.model.Iuv iuvModel = IuvUtils.toIuv(versamento, versamento.getApplicazione(this), versamento.getUo(this).getDominio(this));
					versamento.setNumeroAvviso(iuvModel.getNumeroAvviso());
				}
				
				if(versamento.checkEsecuzioneUpdate(versamentoLetto)) {
					versamento.setAvvisaturaOperazione(AvvisaturaOperazione.UPDATE.getValue());
					versamento.setAvvisaturaDaInviare(true);
					String avvisaturaDigitaleModalitaAnnullamentoAvviso = GovpayConfig.getInstance().getAvvisaturaDigitaleModalitaAnnullamentoAvviso();
					if(!avvisaturaDigitaleModalitaAnnullamentoAvviso.equals(AvvisaturaUtils.AVVISATURA_DIGITALE_MODALITA_USER_DEFINED)) {
						versamento.setAvvisaturaModalita(avvisaturaDigitaleModalitaAnnullamentoAvviso.equals("asincrona") ? ModoAvvisatura.ASICNRONA.getValue() : ModoAvvisatura.SINCRONA.getValue());
					}
				}
				
				ctx.getApplicationLogger().log("versamento.validazioneSemanticaAggiornamento", versamento.getApplicazione(this).getCodApplicazione(), versamento.getCodVersamentoEnte());
				VersamentoUtils.validazioneSemanticaAggiornamento(versamentoLetto, versamento, this);
				ctx.getApplicationLogger().log("versamento.validazioneSemanticaAggiornamentoOk", versamento.getApplicazione(this).getCodApplicazione(), versamento.getCodVersamentoEnte());
				
				
				if(this.isAutoCommit()) {
					this.setAutoCommit(false);
					doCommit = true;
				}
				
				versamentiBD.updateVersamento(versamento, true);
				if(versamento.getId()==null)
					versamento.setId(versamentoLetto.getId());
				
				ctx.getApplicationLogger().log("versamento.aggioramentoOk", versamento.getApplicazione(this).getCodApplicazione(), versamento.getCodVersamentoEnte());
				
				log.info("Versamento (" + versamento.getCodVersamentoEnte() + ") dell'applicazione (" + versamento.getApplicazione(this).getCodApplicazione() + ") aggiornato");
			} catch (NotFoundException e) {
				if(versamento.getNumeroAvviso()!=null) {
					try {
						// 	verifica univocita dell'avviso pagamento prima di inserire il nuovo versamento
						it.govpay.bd.model.Versamento versamentoFromDominioNumeroAvviso = versamentiBD.getVersamentoByDominioIuv(versamento.getDominio(this).getId(), IuvUtils.toIuv(versamento.getNumeroAvviso()));
					
						// due pendenze non possono avere lo stesso numero avviso
						if(!versamentoFromDominioNumeroAvviso.getCodVersamentoEnte().equals(versamento.getCodVersamentoEnte()))
							throw new GovPayException(EsitoOperazione.VER_025, versamento.getApplicazione(this).getCodApplicazione(), versamento.getCodVersamentoEnte(), 
									versamentoFromDominioNumeroAvviso.getApplicazione(this).getCodApplicazione(), versamentoFromDominioNumeroAvviso.getCodVersamentoEnte(),versamento.getNumeroAvviso());
						
					}catch(NotFoundException e2) {
						// ignore
					}
				} else if(generaIuv) {
					// Non ha numero avviso, ma e' richiesto che lo abbia
					Iuv iuvBusiness = new Iuv(this);
					String iuv = iuvBusiness.generaIUV(versamento.getApplicazione(this), versamento.getUo(this).getDominio(this), versamento.getCodVersamentoEnte(), TipoIUV.NUMERICO);
					// imposto iuv calcolato
					versamento.setIuvVersamento(iuv);
					// calcolo il numero avviso
					it.govpay.core.business.model.Iuv iuvModel = IuvUtils.toIuv(versamento, versamento.getApplicazione(this), versamento.getUo(this).getDominio(this));
					versamento.setNumeroAvviso(iuvModel.getNumeroAvviso());
				}
				
				if(this.isAutoCommit()) {
					this.setAutoCommit(false);
					doCommit = true;
				}
				
				// Versamento nuovo. Inserisco versamento ed eventuale promemoria avviso
				versamento.setCreated(true);
				TipoVersamentoDominio tipoVersamentoDominio = versamento.getTipoVersamentoDominio(this);
//				Promemoria promemoria = null;
				
				boolean inserisciNotificaAvviso = false;
				boolean inserisciNotificaPromemoriaScadenzaMail = false;
				boolean inserisciNotificaPromemoriaScadenzaAppIO = false;
				BigDecimal giorniPreavvisoMail = null;
				BigDecimal giorniPreavvisoAppIO = null;
				
				if((tipoVersamentoDominio.getAvvisaturaMailPromemoriaAvvisoAbilitato() || 
						tipoVersamentoDominio.getAvvisaturaAppIoPromemoriaAvvisoAbilitato()) && !(avvisatura != null && avvisatura.booleanValue()==false)) {
					log.debug("Schedulazione invio avviso di pagamento impostata.");
					inserisciNotificaAvviso = true;
				}
				
				if(tipoVersamentoDominio.getAvvisaturaAppIoPromemoriaScadenzaAbilitato() && !(avvisatura != null && avvisatura.booleanValue()==false)) {
					log.debug("Schedulazione invio scadenza avviso di pagamento impostata.");
					inserisciNotificaPromemoriaScadenzaAppIO = true;
					giorniPreavvisoAppIO = tipoVersamentoDominio.getAvvisaturaAppIoPromemoriaScadenzaPreavviso();
					
					if(giorniPreavvisoAppIO == null)
						giorniPreavvisoAppIO = new BigDecimal(AnagraficaManager.getConfigurazione(this).getAvvisaturaViaAppIo().getPromemoriaScadenza().getPreavviso());
				}
				
				if(tipoVersamentoDominio.getAvvisaturaMailPromemoriaScadenzaAbilitato() && !(avvisatura != null && avvisatura.booleanValue()==false)) {
					log.debug("Schedulazione invio scadenza avviso di pagamento impostata.");
					inserisciNotificaPromemoriaScadenzaMail = true;
					giorniPreavvisoMail = tipoVersamentoDominio.getAvvisaturaMailPromemoriaScadenzaPreavviso();
					
					if(giorniPreavvisoMail == null)
						giorniPreavvisoMail = new BigDecimal(AnagraficaManager.getConfigurazione(this).getAvvisaturaViaMail().getPromemoriaScadenza().getPreavviso());
				}
				
				// dataNotificaAvviso e avvisoNotificato
				if(inserisciNotificaAvviso) {
					if(versamento.getDataNotificaAvviso() == null)
						versamento.setDataNotificaAvviso(versamento.getDataCreazione());
				}
				
				if(versamento.getDataNotificaAvviso() != null)
					versamento.setAvvisoNotificato(false);
				
				// dataPromemoriaScadenza e promemoriaNotificato
				if(inserisciNotificaPromemoriaScadenzaMail) {
					if(versamento.getAvvMailDataPromemoriaScadenza() == null) {
						Date dataValidita = versamento.getDataValidita();
						Calendar c = Calendar.getInstance();
						c.setTime(dataValidita);
						c.add(Calendar.DATE, -(giorniPreavvisoMail.intValue()));
						versamento.setAvvMailDataPromemoriaScadenza(c.getTime());
					}
				}
				
				if(versamento.getAvvMailDataPromemoriaScadenza() != null)
					versamento.setAvvMailPromemoriaScadenzaNotificato(false);
				
				if(inserisciNotificaPromemoriaScadenzaAppIO) {
					if(versamento.getAvvAppIODataPromemoriaScadenza() == null) {
						Date dataValidita = versamento.getDataValidita();
						Calendar c = Calendar.getInstance();
						c.setTime(dataValidita);
						c.add(Calendar.DATE, -(giorniPreavvisoAppIO.intValue()));
						versamento.setAvvAppIODataPromemoriaScadenza(c.getTime());
					}
				}
				
				if(versamento.getAvvAppIODataPromemoriaScadenza() != null)
					versamento.setAvvAppIOPromemoriaScadenzaNotificato(false);
			
				// generazione UUID creazione
				versamento.setIdSessione(UUID.randomUUID().toString().replace("-", ""));
				if(versamento.getStatoPagamento() == null) {
					versamento.setStatoPagamento(StatoPagamento.NON_PAGATO);
					versamento.setImportoIncassato(BigDecimal.ZERO);
					versamento.setImportoPagato(BigDecimal.ZERO);
				}
								
				versamentiBD.insertVersamento(versamento);
				ctx.getApplicationLogger().log("versamento.inserimentoOk", versamento.getApplicazione(this).getCodApplicazione(), versamento.getCodVersamentoEnte());
				log.info("Versamento (" + versamento.getCodVersamentoEnte() + ") dell'applicazione (" + versamento.getApplicazione(this).getCodApplicazione() + ") inserito");
				
				// avvio il batch di gestione dei promemoria
				Operazioni.setEseguiGestionePromemoria();
			}
			if(doCommit) this.commit();
			
			return versamento;
		} catch (Exception e) {
			if(doCommit) this.rollback();
			if(e instanceof GovPayException)
				throw (GovPayException) e;
			else 
				throw new GovPayException(e);
		}
	}

	private void copiaPropertiesNonModificabiliVersamento(it.govpay.bd.model.Versamento versamento, it.govpay.bd.model.Versamento versamentoLetto) {
		// riporto informazioni che non si modificano
		versamento.setTipo(versamentoLetto.getTipo());
		versamento.setAvvisaturaAbilitata(versamentoLetto.isAvvisaturaAbilitata());
		versamento.setAvvisaturaCodAvvisatura(versamentoLetto.getAvvisaturaCodAvvisatura());
		versamento.setAvvisaturaDaInviare(versamentoLetto.isAvvisaturaDaInviare());
		versamento.setAvvisaturaModalita(versamentoLetto.getAvvisaturaModalita());
		versamento.setAvvisaturaOperazione(versamentoLetto.getAvvisaturaOperazione());
		versamento.setAvvisaturaTipoPagamento(versamentoLetto.getAvvisaturaTipoPagamento());
		versamento.setAck(versamentoLetto.isAck());
		versamento.setDataCreazione(versamentoLetto.getDataCreazione());
		versamento.setIdTracciatoAvvisatura(versamentoLetto.getIdTracciatoAvvisatura());
		versamento.setIdSessione(versamentoLetto.getIdSessione());
		versamento.setStatoPagamento(versamentoLetto.getStatoPagamento());
		versamento.setImportoPagato(versamentoLetto.getImportoPagato());
		versamento.setImportoIncassato(versamentoLetto.getImportoIncassato());
		versamento.setDataNotificaAvviso(versamentoLetto.getDataNotificaAvviso());
		versamento.setAvvisoNotificato(versamentoLetto.getAvvisoNotificato());
		versamento.setAvvMailDataPromemoriaScadenza(versamentoLetto.getAvvMailDataPromemoriaScadenza()); 
		versamento.setAvvMailPromemoriaScadenzaNotificato(versamentoLetto.getAvvMailPromemoriaScadenzaNotificato());
		versamento.setAvvAppIODataPromemoriaScadenza(versamentoLetto.getAvvAppIODataPromemoriaScadenza()); 
		versamento.setAvvAppIOPromemoriaScadenzaNotificato(versamentoLetto.getAvvAppIOPromemoriaScadenzaNotificato());
		
		// riporto iuv e numero avviso che sono gia' stati assegnati
		if(versamento.getIuvVersamento() == null) {
			versamento.setIuvVersamento(versamentoLetto.getIuvVersamento());
			versamento.setNumeroAvviso(versamentoLetto.getNumeroAvviso());
		}
	}
	
	public void annullaVersamento(AnnullaVersamentoDTO annullaVersamentoDTO) throws GovPayException, NotAuthorizedException, UtilsException {
		log.info("Richiesto annullamento per il Versamento (" + annullaVersamentoDTO.getCodVersamentoEnte() + ") dell'applicazione (" + annullaVersamentoDTO.getCodApplicazione() + ")");
		
		IContext ctx = ContextThreadLocal.get();
		GpContext appContext = (GpContext) ctx.getApplicationContext();
		
		if(!appContext.hasCorrelationId()) appContext.setCorrelationId(annullaVersamentoDTO.getCodApplicazione() + annullaVersamentoDTO.getCodVersamentoEnte());
		appContext.getRequest().addGenericProperty(new Property("codApplicazione", annullaVersamentoDTO.getCodApplicazione()));
		appContext.getRequest().addGenericProperty(new Property("codVersamentoEnte", annullaVersamentoDTO.getCodVersamentoEnte()));
		ctx.getApplicationLogger().log("versamento.annulla");
		
		if(annullaVersamentoDTO.getApplicazione() != null && !annullaVersamentoDTO.getApplicazione().getCodApplicazione().equals(annullaVersamentoDTO.getCodApplicazione())) {
			throw new NotAuthorizedException("Applicazione chiamante [" + annullaVersamentoDTO.getApplicazione().getCodApplicazione() + "] non e' proprietaria del versamento");
		}
		
		String codApplicazione = annullaVersamentoDTO.getCodApplicazione();
		String codVersamentoEnte = annullaVersamentoDTO.getCodVersamentoEnte();
		
		try {
			VersamentiBD versamentiBD = new VersamentiBD(this);
			
			this.setAutoCommit(false);
			this.enableSelectForUpdate();
			
			try {
				it.govpay.bd.model.Versamento versamentoLetto = versamentiBD.getVersamento(AnagraficaManager.getApplicazione(this, codApplicazione).getId(), codVersamentoEnte);
			
				// Il controllo sul dominio disponibile per l'operatore riferito delle pendenze del tracciato e' gia' stato fatto durante l'operazione di caricamento tracciato.
//				if(annullaVersamentoDTO.getOperatore() != null && 
//						!AclEngine.isAuthorized(annullaVersamentoDTO.getOperatore().getUtenza(), Servizio.PAGAMENTI_E_PENDENZE, versamentoLetto.getUo(this).getDominio(this).getCodDominio(), null, Arrays.asList(Diritti.SCRITTURA,Diritti.ESECUZIONE))){
//					throw new NotAuthorizedException("Operatore chiamante [" + annullaVersamentoDTO.getOperatore().getPrincipal() + "] non autorizzato in scrittura per il dominio " + versamentoLetto.getUo(this).getDominio(this).getCodDominio());
//				}
				// Se è già annullato non devo far nulla.
				if(versamentoLetto.getStatoVersamento().equals(StatoVersamento.ANNULLATO)) {
					log.info("Versamento (" + versamentoLetto.getCodVersamentoEnte() + ") dell'applicazione (" + codApplicazione + ") gia' annullato. Aggiornamento non necessario.");
					ctx.getApplicationLogger().log("versamento.annullaOk");
					return;
				}
				
				// Se è in stato NON_ESEGUITO lo annullo ed aggiorno lo stato avvisatura
				if(versamentoLetto.getStatoVersamento().equals(StatoVersamento.NON_ESEGUITO)) {
					versamentoLetto.setStatoVersamento(StatoVersamento.ANNULLATO);
					versamentoLetto.setDescrizioneStato(annullaVersamentoDTO.getMotivoAnnullamento()); 
					versamentoLetto.setAvvisaturaOperazione(AvvisaturaOperazione.DELETE.getValue());
					versamentoLetto.setAvvisaturaDaInviare(true);
					String avvisaturaDigitaleModalitaAnnullamentoAvviso = GovpayConfig.getInstance().getAvvisaturaDigitaleModalitaAnnullamentoAvviso();
					if(!avvisaturaDigitaleModalitaAnnullamentoAvviso.equals(AvvisaturaUtils.AVVISATURA_DIGITALE_MODALITA_USER_DEFINED)) {
						versamentoLetto.setAvvisaturaModalita(avvisaturaDigitaleModalitaAnnullamentoAvviso.equals("asincrona") ? ModoAvvisatura.ASICNRONA.getValue() : ModoAvvisatura.SINCRONA.getValue());
					}
					versamentoLetto.setAvvisoNotificato(null);
					versamentoLetto.setAvvMailPromemoriaScadenzaNotificato(null);
					versamentoLetto.setAvvAppIOPromemoriaScadenzaNotificato(null);
					versamentiBD.updateVersamento(versamentoLetto);
					log.info("Versamento (" + versamentoLetto.getCodVersamentoEnte() + ") dell'applicazione (" + codApplicazione + ") annullato.");
					ctx.getApplicationLogger().log("versamento.annullaOk");
					return;
				}
				
				// Se non è ne ANNULLATO ne NON_ESEGUITO non lo posso annullare
				throw new GovPayException(EsitoOperazione.VER_009, codApplicazione, codVersamentoEnte, versamentoLetto.getStatoVersamento().toString());
				
			} catch (NotFoundException e) {
				// Versamento inesistente
				throw new GovPayException(EsitoOperazione.VER_008, codApplicazione, codVersamentoEnte);
			} finally {
				this.commit();
			}
		} catch (Exception e) {
			this.rollback();
			this.handleAnnullamentoException(ctx, e);
		} finally {
			try {
				this.disableSelectForUpdate();
			} catch (ServiceException e) {
//				GovPayException gpe = new GovPayException(e);
//				ctx.getApplicationLogger().log(LOG_KEY_VERSAMENTO_ANNULLA_KO, gpe.getCodEsito().toString(), gpe.getDescrizioneEsito(), gpe.getCausa() != null ? gpe.getCausa() : ECCEZIONE_NON_SPECIFICATA);
//				throw gpe;
			}
		}
	}
	
	private void handleAnnullamentoException(IContext ctx, Exception e) throws GovPayException, NotAuthorizedException, UtilsException {
		if(e instanceof GovPayException) {
			GovPayException gpe = (GovPayException) e;
			ctx.getApplicationLogger().log(LOG_KEY_VERSAMENTO_ANNULLA_KO, gpe.getCodEsito().toString(), gpe.getDescrizioneEsito(), gpe.getCausa() != null ? gpe.getCausa() : ECCEZIONE_NON_SPECIFICATA);
			throw (GovPayException) e;
		} else if(e instanceof NotAuthorizedException) { 
			NotAuthorizedException nae = (NotAuthorizedException) e;
			ctx.getApplicationLogger().log(LOG_KEY_VERSAMENTO_ANNULLA_KO, "NOT_AUTHORIZED", nae.getDetails(), nae.getMessage() != null ? nae.getMessage() : ECCEZIONE_NON_SPECIFICATA);
			throw nae;
		} else {
			GovPayException gpe = new GovPayException(e);
			ctx.getApplicationLogger().log(LOG_KEY_VERSAMENTO_ANNULLA_KO, gpe.getCodEsito().toString(), gpe.getDescrizioneEsito(), gpe.getCausa() != null ? gpe.getCausa() : ECCEZIONE_NON_SPECIFICATA);
			throw gpe;
		}
	}
	
	public it.govpay.bd.model.Versamento chiediVersamento(RefVersamentoAvviso ref, Dominio dominio) throws ServiceException, GovPayException, UtilsException {
		// conversione numeroAvviso in iuv
		String iuv = VersamentoUtils.getIuvFromNumeroAvviso(ref.getNumeroAvviso());
		return this.chiediVersamento(null, null, null, null, ref.getIdDominio(), iuv, TipologiaTipoVersamento.DOVUTO);	
	}

	public it.govpay.bd.model.Versamento chiediVersamento(RefVersamentoPendenza ref) throws ServiceException, GovPayException, UtilsException {
		return this.chiediVersamento(ref.getIdA2A(), ref.getIdPendenza(), null, null, null, null, TipologiaTipoVersamento.DOVUTO);
	}

	public it.govpay.bd.model.Versamento chiediVersamento(it.govpay.core.dao.commons.Versamento versamento) throws ServiceException, GovPayException, ValidationException { 
		return VersamentoUtils.toVersamentoModel(versamento, this);
	}

	public it.govpay.bd.model.Versamento chiediVersamento(String codApplicazione, String codVersamentoEnte, String bundlekey, String codUnivocoDebitore, String codDominio, String iuv, TipologiaTipoVersamento tipo) throws ServiceException, GovPayException, UtilsException {
		IContext ctx = ContextThreadLocal.get();
		// Versamento per riferimento codApplicazione/codVersamentoEnte
		it.govpay.bd.model.Versamento versamentoModel = null;
		
		VersamentiBD versamentiBD = new VersamentiBD(this);
		
		if(codApplicazione != null && codVersamentoEnte != null) {
			ctx.getApplicationLogger().log("rpt.acquisizioneVersamentoRef", codApplicazione, codVersamentoEnte);
			Applicazione applicazione = null;
			try {
				applicazione = AnagraficaManager.getApplicazione(this, codApplicazione);
			} catch (NotFoundException e) {
				throw new GovPayException(EsitoOperazione.APP_000, codApplicazione);
			}
			
			if(!applicazione.getUtenza().isAbilitato())
				throw new GovPayException(EsitoOperazione.APP_001, applicazione.getCodApplicazione());

			try {
				versamentoModel = versamentiBD.getVersamento(applicazione.getId(), codVersamentoEnte);
				versamentoModel.setIuvProposto(iuv);
			} catch (NotFoundException e) {
				// Non e' nel repo interno. vado oltre e lo richiedo all'applicazione gestrice
			}
		}


		// Versamento per riferimento codDominio/iuv
		if(codDominio != null && iuv != null) {
			ctx.getApplicationLogger().log("rpt.acquisizioneVersamentoRefIuv", codDominio, iuv);

			Dominio dominio = null;
			try {
				dominio = AnagraficaManager.getDominio(this, codDominio);
			} catch (NotFoundException e) {
				throw new GovPayException(EsitoOperazione.DOM_000, codDominio);
			}
			
			if(!dominio.isAbilitato())
				throw new GovPayException(EsitoOperazione.DOM_001, dominio.getCodDominio());

			try {
				versamentoModel = versamentiBD.getVersamentoByDominioIuv(dominio.getId(), iuv);
				codApplicazione = versamentoModel.getApplicazione(this).getCodApplicazione();
				codVersamentoEnte = versamentoModel.getCodVersamentoEnte();
			} catch (NotFoundException e) {
				// Iuv non registrato. Vedo se c'e' un'applicazione da interrogare, altrimenti non e' recuperabile.
				Applicazione applicazioneDominio = new it.govpay.core.business.Applicazione(this).getApplicazioneDominio(dominio, iuv, false);
				if(applicazioneDominio == null) {
					throw new GovPayException("L'avviso di pagamento [Dominio:" + codDominio + " Iuv:" + iuv + "] non risulta registrato, ne associabile ad un'applicazione censita.", EsitoOperazione.VER_008);
				}
				codApplicazione = applicazioneDominio.getCodApplicazione();
			}

			// A questo punto ho sicuramente il codApplicazione. Se ho anche il codVersamentoEnte lo cerco localmente
			if(codVersamentoEnte != null) {
				try {
					versamentoModel = versamentiBD.getVersamento(AnagraficaManager.getApplicazione(this, codApplicazione).getId(), codVersamentoEnte);
				} catch (NotFoundException e) {
					// Non e' nel repo interno. vado oltre e lo richiedo all'applicazione gestrice
				}
			}
		}
			
		// Versamento per riferimento codApplicazione/bundlekey
		if(codApplicazione != null && bundlekey != null) {
			ctx.getApplicationLogger().log("rpt.acquisizioneVersamentoRefBundle", codApplicazione, bundlekey, (codDominio != null ? codDominio : GpContext.NOT_SET), (codUnivocoDebitore != null ? codUnivocoDebitore : GpContext.NOT_SET));
			try {
				versamentoModel = versamentiBD.getVersamentoByBundlekey(AnagraficaManager.getApplicazione(this, codApplicazione).getId(), bundlekey, codDominio, codUnivocoDebitore);
			} catch (NotFoundException e) {
				// Non e' nel repo interno. vado oltre e lo richiedo all'applicazione gestrice
			}
		}
			
		// Se ancora non ho trovato il versamento, lo chiedo all'applicazione
		if(versamentoModel == null) {
			try {
				versamentoModel = VersamentoUtils.acquisisciVersamento(AnagraficaManager.getApplicazione(this, codApplicazione), codVersamentoEnte, bundlekey, codUnivocoDebitore, codDominio, iuv, tipo, this);
			} catch (ClientException e){
				throw new GovPayException(EsitoOperazione.INTERNAL, "verifica del versamento [Versamento: " + codVersamentoEnte != null ? codVersamentoEnte : "-" + " BundleKey:" + bundlekey != null ? bundlekey : "-" + " Debitore:" + codUnivocoDebitore != null ? codUnivocoDebitore : "-" + " Dominio:" + codDominio != null ? codDominio : "-" + " Iuv:" + iuv != null ? iuv : "-" + "] all'applicazione competente [Applicazione:" + codApplicazione + "] e' fallita con errore: " + e.getMessage());
			} catch (VersamentoScadutoException e) {
				throw new GovPayException("La verifica del versamento [Versamento: " + codVersamentoEnte != null ? codVersamentoEnte : "-" + " BundleKey:" + bundlekey != null ? bundlekey : "-" + " Debitore:" + codUnivocoDebitore != null ? codUnivocoDebitore : "-" + " Dominio:" + codDominio != null ? codDominio : "-" + " Iuv:" + iuv != null ? iuv : "-" + "] all'applicazione competente [Applicazione:" + codApplicazione + "] ha dato esito PAA_PAGAMENTO_SCADUTO", EsitoOperazione.VER_010);
			} catch (VersamentoAnnullatoException e) {
				throw new GovPayException("La verifica del versamento [Versamento: " + codVersamentoEnte != null ? codVersamentoEnte : "-" + " BundleKey:" + bundlekey != null ? bundlekey : "-" + " Debitore:" + codUnivocoDebitore != null ? codUnivocoDebitore : "-" + " Dominio:" + codDominio != null ? codDominio : "-" + " Iuv:" + iuv != null ? iuv : "-" + "] all'applicazione competente [Applicazione:" + codApplicazione + "] ha dato esito PAA_PAGAMENTO_ANNULLATO", EsitoOperazione.VER_013);
			} catch (VersamentoDuplicatoException e) {
				throw new GovPayException("La verifica del versamento [Versamento: " + codVersamentoEnte != null ? codVersamentoEnte : "-" + " BundleKey:" + bundlekey != null ? bundlekey : "-" + " Debitore:" + codUnivocoDebitore != null ? codUnivocoDebitore : "-" + " Dominio:" + codDominio != null ? codDominio : "-" + " Iuv:" + iuv != null ? iuv : "-" + "] all'applicazione competente [Applicazione:" + codApplicazione + "] ha dato esito PAA_PAGAMENTO_DUPLICATO", EsitoOperazione.VER_012);
			} catch (VersamentoSconosciutoException e) {
				throw new GovPayException("La verifica del versamento [Versamento: " + codVersamentoEnte != null ? codVersamentoEnte : "-" + " BundleKey:" + bundlekey != null ? bundlekey : "-" + " Debitore:" + codUnivocoDebitore != null ? codUnivocoDebitore : "-" + " Dominio:" + codDominio != null ? codDominio : "-" + " Iuv:" + iuv != null ? iuv : "-" + "] all'applicazione competente [Applicazione:" + codApplicazione + "] ha dato esito PAA_PAGAMENTO_SCONOSCIUTO", EsitoOperazione.VER_011);
			} catch (NotFoundException e) {
				throw new GovPayException(EsitoOperazione.INTERNAL, "Il versamento [Versamento: " + codVersamentoEnte != null ? codVersamentoEnte : "-" + " BundleKey:" + bundlekey != null ? bundlekey : "-" + " Debitore:" + codUnivocoDebitore != null ? codUnivocoDebitore : "-" + " Dominio:" + codDominio != null ? codDominio : "-" + " Iuv:" + iuv != null ? iuv : "-" + "] e' gestito da un'applicazione non censita [Applicazione:" + codApplicazione + "]");
			} catch (VersamentoNonValidoException e) { 
				throw new GovPayException(EsitoOperazione.INTERNAL, "verifica del versamento [Versamento: " + codVersamentoEnte != null ? codVersamentoEnte : "-" + " BundleKey:" + bundlekey != null ? bundlekey : "-" + " Debitore:" + codUnivocoDebitore != null ? codUnivocoDebitore : "-" + " Dominio:" + codDominio != null ? codDominio : "-" + " Iuv:" + iuv != null ? iuv : "-" + "] all'applicazione competente [Applicazione:" + codApplicazione + "] e' fallita con errore: " + e.getMessage());
			}
		}
		
		return versamentoModel;
	}

	public void inserisciPromemoriaScadenzaMail(it.govpay.bd.model.Versamento versamento) throws ServiceException {
		boolean wasAutocommit = this.isAutoCommit();

		if(this.isAutoCommit()) {
			this.setAutoCommit(false);
		}
		
		String codApplicazione = versamento.getApplicazione(this).getCodApplicazione();
		String codVersamentoEnte = versamento.getCodVersamentoEnte();
		Promemoria promemoria = null;
		try {
			log.debug("Inserimento promemoria scadenza per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"]");
			TipoVersamentoDominio tipoVersamentoDominio = versamento.getTipoVersamentoDominio(this);
			
			if(tipoVersamentoDominio.getAvvisaturaMailPromemoriaScadenzaAbilitato()) {
				log.debug("Schedulazione invio avviso di scadenza pagamento in corso...");
				it.govpay.core.business.Promemoria promemoriaBD = new it.govpay.core.business.Promemoria(this);
				promemoria = promemoriaBD.creaPromemoriaScadenza(versamento, tipoVersamentoDominio, null);
				String msg = "e' stato trovato un destinatario valido, l'invio e' stato schedulato con successo.";
				if(promemoria.getDestinatarioTo() == null) {
					msg = "non e' stato trovato un destinatario valido, l'invio non verra' schedulato.";
					promemoria = null;
				}
				log.debug("Creazione promemoria scadenza completata: "+ msg);
			}
			
			// promemoria mail
			if(promemoria != null) {
				if(versamento.getIdDocumento() == null)
					promemoria.setIdVersamento(versamento.getId());
				else 
					promemoria.setIdDocumento(versamento.getIdDocumento());
				
				PromemoriaBD promemoriaBD = new PromemoriaBD(this);
				promemoriaBD.insertPromemoria(promemoria);
			}
			
			// aggiornamento stato notifica versamento
			versamento.setAvvMailPromemoriaScadenzaNotificato(true);
			VersamentiBD versamentiBD = new VersamentiBD(this);
			versamentiBD.updateStatoPromemoriaScadenzaMailVersamento(versamento.getId(), true, true);
			
			if(!this.isAutoCommit()) this.commit();
			
			log.debug("Inserimento promemoria scadenza per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"] completato");
		} catch(Throwable e) {
			log.error("Errore durante l'inserimento promemoria scadenza per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"]: " + e.getMessage(),e);
			if(!this.isAutoCommit()) this.rollback();
		} finally {
			this.setAutoCommit(wasAutocommit);
		}
	}
	
	public void inserisciPromemoriaScadenzaAppIO(it.govpay.bd.model.Versamento versamento) throws ServiceException {
		boolean wasAutocommit = this.isAutoCommit();

		if(this.isAutoCommit()) {
			this.setAutoCommit(false);
		}
		
		String codApplicazione = versamento.getApplicazione(this).getCodApplicazione();
		String codVersamentoEnte = versamento.getCodVersamentoEnte();
		it.govpay.bd.model.NotificaAppIo notificaAppIo = null;
		try {
			log.debug("Inserimento promemoria scadenza per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"]");
			TipoVersamentoDominio tipoVersamentoDominio = versamento.getTipoVersamentoDominio(this);
			
			if(tipoVersamentoDominio.getAvvisaturaAppIoPromemoriaScadenzaAbilitato()) {
				notificaAppIo = new NotificaAppIo(versamento, TipoNotifica.SCADENZA, this);
			}
			
			// notifica AppIO
			if(notificaAppIo != null) {
				notificaAppIo.setIdVersamento(versamento.getId());
				NotificheAppIoBD notificheAppIoBD = new NotificheAppIoBD(this);
				notificheAppIoBD.insertNotifica(notificaAppIo);
			}
			
			// aggiornamento stato notifica versamento
			versamento.setAvvAppIOPromemoriaScadenzaNotificato(true);
			VersamentiBD versamentiBD = new VersamentiBD(this);
			versamentiBD.updateStatoPromemoriaScadenzaAppIOVersamento(versamento.getId(), true, true);
			
			if(!this.isAutoCommit()) this.commit();
			
			log.debug("Inserimento promemoria scadenza per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"] completato");
		} catch(Throwable e) {
			log.error("Errore durante l'inserimento promemoria scadenza per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"]: " + e.getMessage(),e);
			if(!this.isAutoCommit()) this.rollback();
		} finally {
			this.setAutoCommit(wasAutocommit);
		}
	}

	public void inserisciPromemoriaAvviso(it.govpay.bd.model.Versamento versamento) throws ServiceException{
		boolean wasAutocommit = this.isAutoCommit();
		
		if(this.isAutoCommit()) {
			this.setAutoCommit(false);
		}

		String codApplicazione = versamento.getApplicazione(this).getCodApplicazione();
		String codVersamentoEnte = versamento.getCodVersamentoEnte();
		Promemoria promemoria = null;
		it.govpay.bd.model.NotificaAppIo notificaAppIo = null;
		
		try {
			log.debug("Inserimento promemoria avviso per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"]");
			TipoVersamentoDominio tipoVersamentoDominio = versamento.getTipoVersamentoDominio(this);
		
			if(tipoVersamentoDominio.getAvvisaturaMailPromemoriaAvvisoAbilitato()) {
				log.debug("Schedulazione invio avviso di pagamento in corso...");
				it.govpay.core.business.Promemoria promemoriaBD = new it.govpay.core.business.Promemoria(this);
				promemoria = promemoriaBD.creaPromemoriaAvviso(versamento, tipoVersamentoDominio, null);
				
				String msg = "e' stato trovato un destinatario valido, l'invio e' stato schedulato con successo.";
				if(promemoria.getDestinatarioTo() == null) {
					msg = "non e' stato trovato un destinatario valido, l'invio non verra' schedulato.";
					promemoria = null;
				}
				log.debug("Creazione promemoria avviso completata: "+ msg);
			}
			
			if(tipoVersamentoDominio.getAvvisaturaAppIoPromemoriaAvvisoAbilitato()) {
				log.debug("Creo notifica avvisatura tramite App IO...");
				notificaAppIo = new NotificaAppIo(versamento, TipoNotifica.AVVISO, this);
				log.debug("Creazione notifica avvisatura tramite App IO completata.");
			}
			// promemoria mail
			if(promemoria != null) {
				if(versamento.getIdDocumento() == null)
					promemoria.setIdVersamento(versamento.getId());
				else 
					promemoria.setIdDocumento(versamento.getIdDocumento());
				
				PromemoriaBD promemoriaBD = new PromemoriaBD(this);
				promemoriaBD.insertPromemoria(promemoria);
			}
			
			// notifica AppIO
			if(notificaAppIo != null) {
				notificaAppIo.setIdVersamento(versamento.getId());
				NotificheAppIoBD notificheAppIoBD = new NotificheAppIoBD(this);
				notificheAppIoBD.insertNotifica(notificaAppIo);
			}
			
			// aggiornamento stato notifica versamento
			versamento.setAvvisoNotificato(true);
			VersamentiBD versamentiBD = new VersamentiBD(this);
			versamentiBD.updateStatoPromemoriaAvvisoVersamento(versamento.getId(), true, true);
			
			if(!this.isAutoCommit()) this.commit();
			
			log.debug("Inserimento promemoria avviso per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"] completato");
		} catch(Throwable e) {
			log.error("Errore durante l'inserimento promemoria avviso per il versamento [IdA2A" + codApplicazione +", CodVersamentoEnte "+codVersamentoEnte+"]: " + e.getMessage(),e);
			if(!this.isAutoCommit()) this.rollback();
		} finally {
			this.setAutoCommit(wasAutocommit);
		}
	}
}
