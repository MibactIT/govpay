/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC 
 * http://www.gov4j.it/govpay
 * 
 * Copyright (c) 2014-2018 Link.it srl (http://www.link.it).
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.UnmarshalException;

import org.apache.commons.lang.StringUtils;
import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;
import org.openspcoop2.utils.LoggerWrapperFactory;
import org.openspcoop2.utils.serialization.IDeserializer;
import org.openspcoop2.utils.serialization.ISerializer;
import org.openspcoop2.utils.serialization.SerializationConfig;
import org.openspcoop2.utils.serialization.SerializationFactory;
import org.openspcoop2.utils.serialization.SerializationFactory.SERIALIZATION_TYPE;
import org.openspcoop2.utils.service.context.IContext;
import org.openspcoop2.utils.sonde.Sonda;
import org.openspcoop2.utils.sonde.SondaException;
import org.openspcoop2.utils.sonde.SondaFactory;
import org.openspcoop2.utils.sonde.impl.SondaBatch;
import org.slf4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import gov.telematici.pagamenti.ws.avvisi_digitali.CtEsitoAvvisatura;
import gov.telematici.pagamenti.ws.avvisi_digitali.CtEsitoAvvisoDigitale;
import gov.telematici.pagamenti.ws.presa_in_carico.EsitoPresaInCarico;
import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.anagrafica.ApplicazioniBD;
import it.govpay.bd.anagrafica.BatchBD;
import it.govpay.bd.anagrafica.IntermediariBD;
import it.govpay.bd.anagrafica.filters.ApplicazioneFilter;
import it.govpay.bd.configurazione.model.AppIOBatch;
import it.govpay.bd.configurazione.model.MailBatch;
import it.govpay.bd.model.Dominio;
import it.govpay.bd.model.EsitoAvvisatura;
import it.govpay.bd.model.Notifica;
import it.govpay.bd.model.NotificaAppIo;
import it.govpay.bd.model.SingoloVersamento;
import it.govpay.bd.model.Tracciato;
import it.govpay.bd.model.Versamento;
import it.govpay.bd.pagamento.EsitiAvvisaturaBD;
import it.govpay.bd.pagamento.TracciatiBD;
import it.govpay.bd.pagamento.VersamentiBD;
import it.govpay.bd.pagamento.filters.EsitoAvvisaturaFilter;
import it.govpay.bd.pagamento.filters.TracciatoFilter;
import it.govpay.bd.pagamento.filters.VersamentoFilter;
import it.govpay.bd.pagamento.util.CountPerDominio;
import it.govpay.core.beans.tracciati.Avvisatura;
import it.govpay.core.business.Rendicontazioni.DownloadRendicontazioniResponse;
import it.govpay.core.dao.pagamenti.dto.ElaboraTracciatoDTO;
import it.govpay.core.utils.AvvisaturaUtils;
import it.govpay.core.utils.GovpayConfig;
import it.govpay.core.utils.SimpleDateFormatUtils;
import it.govpay.core.utils.thread.InviaAvvisaturaThread;
import it.govpay.core.utils.thread.InviaNotificaAppIoThread;
import it.govpay.core.utils.thread.InviaNotificaThread;
import it.govpay.core.utils.thread.ThreadExecutorManager;
import it.govpay.model.Batch;
import it.govpay.model.ConnettoreSftp;
import it.govpay.model.Intermediario;
import it.govpay.model.Tracciato.STATO_ELABORAZIONE;
import it.govpay.model.Tracciato.TIPO_TRACCIATO;
import it.govpay.model.Versamento.ModoAvvisatura;

public class Operazioni{

	private static Logger log = LoggerWrapperFactory.getLogger(Operazioni.class);
	public static final String RND = "update-rnd";
	public static final String PSP = "update-psp";
	public static final String PND = "update-pnd";
	public static final String NTFY = "update-ntfy";
	public static final String NTFY_APP_IO = "update-ntfy-appio";
	public static final String CHECK_NTFY = "check-ntfy";
	public static final String CHECK_NTFY_APP_IO = "check-ntfy-appio";
	public static final String BATCH_TRACCIATI = "caricamento-tracciati";
	public static final String CHECK_TRACCIATI = "check-tracciati";
	public static final String BATCH_GENERAZIONE_AVVISI = "generazione-avvisi";
	public static final String BATCH_AVVISATURA_DIGITALE = "avvisatura-digitale";
	public static final String BATCH_ESITO_AVVISATURA_DIGITALE = "esito-avvisatura-digitale";
	public static final String BATCH_AVVISATURA_DIGITALE_SINCRONA = "avvisatura-digitale-immediata";
	public static final String BATCH_SPEDIZIONE_PROMEMORIA = "spedizione-promemoria";
	public static final String CHECK_RESET_CACHE = "check-reset-cache";
	public static final String BATCH_RESET_CACHE = "reset-cache";
	public static final String CACHE_ANAGRAFICA_GOVPAY = "cache-anagrafica";
	public static final String BATCH_GESTIONE_PROMEMORIA = "gestione-promemoria";

	private static boolean eseguiGenerazioneAvvisi;

	public static synchronized void setEseguiGenerazioneAvvisi() {
		eseguiGenerazioneAvvisi = true;
	}

	public static synchronized void resetEseguiGenerazioneAvvisi() {
		eseguiGenerazioneAvvisi = false;
	}

	public static synchronized boolean getEseguiGenerazioneAvvisi() {
		return eseguiGenerazioneAvvisi;
	}

	private static boolean eseguiElaborazioneTracciati = true;

	public static synchronized void setEseguiElaborazioneTracciati() {
		eseguiElaborazioneTracciati = true;
	}

	public static synchronized void resetEseguiElaborazioneTracciati() {
		eseguiElaborazioneTracciati = false;
	}

	public static synchronized boolean getEseguiElaborazioneTracciati() {
		return eseguiElaborazioneTracciati;
	}

	public static String acquisizioneRendicontazioni(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());
			if(BatchManager.startEsecuzione(bd, RND)) {
				DownloadRendicontazioniResponse downloadRendicontazioni = new Rendicontazioni(bd).downloadRendicontazioni(false);
				aggiornaSondaOK(RND, bd,ctx);
				return downloadRendicontazioni.getDescrizioneEsito();
			} else {
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			log.error("Acquisizione rendicontazioni fallita", e);
			aggiornaSondaKO(RND, e, bd,ctx);
			return "Acquisizione fallita#" + e;
		} finally {
			BatchManager.stopEsecuzione(bd, RND);
			if(bd != null) bd.closeConnection();
		}
	}

	public static String recuperoRptPendenti(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());
			if(BatchManager.startEsecuzione(bd, PND)) {
				String verificaTransazioniPendenti = new Pagamento(bd).verificaTransazioniPendenti();
				aggiornaSondaOK(PND, bd,ctx);
				return verificaTransazioniPendenti;
			} else {
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			log.error("Acquisizione Rpt pendenti fallita", e);
			aggiornaSondaKO(PND, e, bd,ctx);
			return "Acquisizione fallita#" + e;
		} finally {
			BatchManager.stopEsecuzione(bd, PND);
			if(bd != null) bd.closeConnection();
		}
	}

	public static String spedizioneNotifiche(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());

			if(BatchManager.startEsecuzione(bd, NTFY)) {
				log.trace("Spedizione notifiche non consegnate");

				ApplicazioniBD applicazioniBD = new ApplicazioniBD(bd);
				ApplicazioneFilter applicazioniFilter = applicazioniBD.newFilter();

				List<String> applicazioni = applicazioniBD.findAllCodApplicazione(applicazioniFilter);
				it.govpay.core.business.Notifica notificheBD = new it.govpay.core.business.Notifica(bd);

				int threadNotificaPoolSize = GovpayConfig.getInstance().getDimensionePoolNotifica();

				for (String codApplicazione : applicazioni) {
					it.govpay.bd.model.Applicazione applicazione = null;
					try {
						applicazione = AnagraficaManager.getApplicazione(bd, codApplicazione);
					}catch(NotFoundException e) {
						log.debug("Applicazione ["+codApplicazione+"] non trovata, passo alla prossima applicazione.");
						continue;
					}

					// effettuo la spedizione solo per le applicazioni che hanno il connettore configurato.
					if(applicazione.getConnettoreIntegrazione() != null) {
						int offset = 0;
						int limit = (2 * threadNotificaPoolSize);
						List<InviaNotificaThread> threads = new ArrayList<>();
						List<Notifica> notifiche  = notificheBD.findNotificheDaSpedire(offset,limit,codApplicazione);

						log.info("Trovate ["+notifiche.size()+"] notifiche da spedire per l'applicazione ["+codApplicazione+"]");

						if(notifiche.size() > 0) {
							for(Notifica notifica: notifiche) {
								InviaNotificaThread sender = new InviaNotificaThread(notifica, bd,ctx);
								ThreadExecutorManager.getClientPoolExecutorNotifica().execute(sender);
								threads.add(sender);
							}

							log.info("Processi di spedizione notifiche per l'applicazione ["+codApplicazione+"] avviati.");
							aggiornaSondaOK(NTFY, bd,ctx);
							
							// chiudo connessione in attesa che tutti abbiano finito
							bd.closeConnection();

							// Aspetto che abbiano finito tutti
							int numeroErrori = 0;
							while(true){
								try {
									Thread.sleep(2000);
								} catch (InterruptedException e) {

								}
								boolean completed = true;
								for(InviaNotificaThread sender : threads) {
									if(!sender.isCompleted()) 
										completed = false;
								}

								if(completed) { 
									for(InviaNotificaThread sender : threads) {
										if(sender.isErrore()) 
											numeroErrori ++;
									}
									int numOk = threads.size() - numeroErrori;
									log.debug("Completata Esecuzione dei ["+threads.size()+"] Threads, OK ["+numOk+"], Errore ["+numeroErrori+"]");
									break; // esco
								}
							}
							
							// Hanno finito tutti. riattivo la connessione
							bd.setupConnection(ctx.getTransactionId());
							BatchManager.aggiornaEsecuzione(bd, NTFY);
						}
					}   else {
						log.debug("Connettore non configurato per l'applicazione ["+codApplicazione+"], non ricerco notifiche da spedire.");
					}
				}
				aggiornaSondaOK(NTFY, bd,ctx);
				log.info("Spedizione notifiche completata.");
				return "Spedizione notifiche completata.";
			} else {
				log.info("Operazione in corso su altro nodo. Richiesta interrotta.");
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			log.error("Non è stato possibile avviare la spedizione delle notifiche", e);
			aggiornaSondaKO(NTFY, e, bd,ctx); 
			return "Non è stato possibile avviare la spedizione delle notifiche: " + e;
		} finally {
			BatchManager.stopEsecuzione(bd, NTFY);
			if(bd != null) bd.closeConnection();
		}
	}
	
	public static String spedizioneNotificheAppIO(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());
			
			it.govpay.bd.model.Configurazione configurazione = AnagraficaManager.getConfigurazione(bd);
			
			if(!configurazione.getBatchSpedizioneAppIo().isAbilitato()) {
				return "Spedizione notifiche AppIO disabilitata.";
			}

			if(BatchManager.startEsecuzione(bd, NTFY_APP_IO)) {
				log.trace("Spedizione notifiche AppIO non consegnate");

				it.govpay.core.business.NotificaAppIo notificheBD = new it.govpay.core.business.NotificaAppIo(bd);

				int threadNotificaPoolSize = GovpayConfig.getInstance().getDimensionePoolNotificaAppIO();
				int offset = 0;
				int limit = (2 * threadNotificaPoolSize);
				List<InviaNotificaAppIoThread> threads = new ArrayList<>();
				List<NotificaAppIo> notifiche  = notificheBD.findNotificheDaSpedire(offset,limit);

				log.info("Trovate ["+notifiche.size()+"] notifiche AppIO da spedire");

				if(notifiche.size() > 0) {
					for(NotificaAppIo notifica: notifiche) {
						InviaNotificaAppIoThread sender = new InviaNotificaAppIoThread(notifica, bd,ctx);
						ThreadExecutorManager.getClientPoolExecutorNotificaAppIo().execute(sender);
						threads.add(sender);
					}

					log.info("Processi di spedizione notifiche AppIO avviati.");
					aggiornaSondaOK(NTFY_APP_IO, bd,ctx);
					
					// chiudo connessione in attesa che tutti abbiano finito
					bd.closeConnection();

					// Aspetto che abbiano finito tutti
					int numeroErrori = 0;
					while(true){
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {

						}
						boolean completed = true;
						for(InviaNotificaAppIoThread sender : threads) {
							if(!sender.isCompleted()) 
								completed = false;
						}

						if(completed) { 
							for(InviaNotificaAppIoThread sender : threads) {
								if(sender.isErrore()) 
									numeroErrori ++;
							}
							int numOk = threads.size() - numeroErrori;
							log.debug("Completata Esecuzione dei ["+threads.size()+"] Threads, OK ["+numOk+"], Errore ["+numeroErrori+"]");
							break; // esco
						}
					}
					
					// Hanno finito tutti. riattivo la connessione
					bd.setupConnection(ctx.getTransactionId());
					BatchManager.aggiornaEsecuzione(bd, NTFY_APP_IO);
				}
				aggiornaSondaOK(NTFY, bd,ctx);
				log.info("Spedizione notifiche AppIO completata.");
				return "Spedizione notifiche AppIO completata.";
			} else {
				log.info("Operazione in corso su altro nodo. Richiesta interrotta.");
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			log.error("Non è stato possibile avviare la spedizione delle notifiche AppIO", e);
			aggiornaSondaKO(NTFY_APP_IO, e, bd,ctx); 
			return "Non è stato possibile avviare la spedizione delle notifiche AppIO: " + e;
		} finally {
			BatchManager.stopEsecuzione(bd, NTFY_APP_IO);
			if(bd != null) bd.closeConnection();
		}
	}

	public static String resetCacheAnagrafica(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());
			BatchBD batchBD = new BatchBD(bd);
			Batch batch = batchBD.get(CACHE_ANAGRAFICA_GOVPAY);
			batch.setAggiornamento(new Date());
			batchBD.update(batch);
			AnagraficaManager.cleanCache();
			log.info("Aggiornamento della data di reset della cache anagrafica del sistema completato con successo.");	
			return "Aggiornamento della data di reset della cache anagrafica del sistema completato con successo.";
		} catch (Exception e) {
			log.error("Aggiornamento della data di reset cache anagrafica del sistema fallita", e);
			return "Aggiornamento della data di reset cache del sistema fallita: " + e;
		} finally {
			if(bd != null) bd.closeConnection();
		}
	}

	public static String resetCacheAnagraficaCheck(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());
			log.debug("Check reset della cache anagrafica locale in corso ...");	

			BatchBD batchBD = new BatchBD(bd);
			Batch batch = batchBD.get(CACHE_ANAGRAFICA_GOVPAY);
			Date aggiornamento = batch.getAggiornamento();

			Date dataResetAttuale = AnagraficaManager.getDataReset();
			if(aggiornamento != null && dataResetAttuale.getTime() < aggiornamento.getTime()) {
				String clusterId = GovpayConfig.getInstance().getClusterId();
				if(StringUtils.isEmpty(clusterId))
					clusterId = "1";

				log.info("Nodo ["+clusterId+"]: Reset della cache anagrafica locale in corso...");	
				AnagraficaManager.cleanCache();
				log.info("Nodo ["+clusterId+"]: Reset della cache anagrafica locale completato.");
			}

			log.debug("Check reset della cache anagrafica locale completato con successo.");	
			return "Check reset della cache anagrafica locale completato con successo.";
		} catch (Exception e) {
			log.error("Check reset della cache anagrafica locale fallito", e);
			return "Check reset della cache anagrafica locale fallito: " + e;
		} finally {
			if(bd != null) bd.closeConnection();
		}
	}

	private static void aggiornaSondaOK(String nome, BasicBD bd, IContext ctx) {
		if(bd==null) return;
		boolean wasConnected = true;
		try {
			if(bd.isClosed()) {
				wasConnected = false;
				bd.setupConnection(ctx.getTransactionId());
			}
			bd.enableSelectForUpdate();
			Connection con = bd.getConnection();

			Sonda sonda = SondaFactory.get(nome, con, bd.getJdbcProperties().getDatabase());
			if(sonda == null) throw new SondaException("Sonda ["+nome+"] non trovata");
			//			Properties properties = new Properties();
			//			((SondaBatch)sonda).aggiornaStatoSonda(true, properties, new Date(), "Ok", con, bd.getJdbcProperties().getDatabase());
			((SondaBatch)sonda).aggiornaStatoSonda(true,  new Date(), "Ok", con, bd.getJdbcProperties().getDatabase());
		} catch (Throwable t) {
			log.warn("Errore nell'aggiornamento della sonda OK: "+ t.getMessage());
		}
		finally {
			if(bd != null)
				try {
					bd.disableSelectForUpdate();
				} catch (ServiceException e) {
					log.error("Errore " +e.getMessage() , e);
				}
			if(bd != null && !wasConnected) bd.closeConnection();
		}
	}

	private static void aggiornaSondaKO(String nome, Exception e, BasicBD bd, IContext ctx) {
		if(bd==null) return;
		boolean wasConnected = true;
		try {
			if(bd.isClosed()) {
				wasConnected = false;
				bd.setupConnection(ctx.getTransactionId());
			}
			bd.enableSelectForUpdate();
			Connection con = bd.getConnection();
			Sonda sonda = SondaFactory.get(nome, con, bd.getJdbcProperties().getDatabase());
			if(sonda == null) throw new SondaException("Sonda ["+nome+"] non trovata");
			//			Properties properties = new Properties();
			//			((SondaBatch)sonda).aggiornaStatoSonda(false, properties, new Date(), "Il batch e' stato interrotto con errore: " + e.getMessage(), con, bd.getJdbcProperties().getDatabase());
			((SondaBatch)sonda).aggiornaStatoSonda(false, new Date(), "Il batch e' stato interrotto con errore: " + e.getMessage(), con, bd.getJdbcProperties().getDatabase());
		} catch (Throwable t) {
			log.warn("Errore nell'aggiornamento della sonda KO: "+ t.getMessage());
		} finally {
			if(bd != null)
				try {
					bd.disableSelectForUpdate();
				} catch (ServiceException e1) {
					log.error("Errore " +e1.getMessage() , e1);
				}
			if(bd != null && !wasConnected) bd.closeConnection();
		}
	}

	@SuppressWarnings("unchecked")
	public static String esitoAvvisaturaDigitale(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());
			if(BatchManager.startEsecuzione(bd, BATCH_ESITO_AVVISATURA_DIGITALE)) {
				log.trace("Batch esito avvisatura digitale");

				SerializationConfig config = new SerializationConfig();
				config.setDf(SimpleDateFormatUtils.newSimpleDateFormatDataOreMinuti());
				config.setIgnoreNullValues(true);

				IDeserializer deserializer = SerializationFactory.getDeserializer(SERIALIZATION_TYPE.JSON_JACKSON, config);
				ISerializer serializer = SerializationFactory.getSerializer(SERIALIZATION_TYPE.JSON_JACKSON, config);

				List<Intermediario>	intermediari = new IntermediariBD(bd).getIntermediari();
				TracciatiBD tracciatiBD = new TracciatiBD(bd);

				for(Intermediario intermediario : intermediari) {

					if(intermediario.getConnettoreSftp()!=null) {
						// Accedo al server sftp e cerco nuovi flussi di esito avvisatura
						log.debug("Accedo al servizio SFTP per l'intermediario " + intermediario.getCodIntermediario());
						JSch jsch = new JSch();
						Session sessionIn = null;
						Channel channelIn = null;
						try {
							sessionIn = jsch.getSession(intermediario.getConnettoreSftp().getHttpUserIn(), intermediario.getConnettoreSftp().getHostIn(), Integer.parseInt(intermediario.getConnettoreSftp().getPortaIn())); 
							sessionIn.setConfig("StrictHostKeyChecking", "no");
							sessionIn.setPassword(intermediario.getConnettoreSftp().getHttpPasswIn());
							sessionIn.connect();

							channelIn = sessionIn.openChannel("sftp");
							channelIn.connect();
							ChannelSftp sftpChannel = (ChannelSftp) channelIn;

							// Cerco file di richiesta Avvisatura che finiscono per _AV.zip
							log.debug("Cerco tracciati di Avvisatura digitale...");
							Vector<ChannelSftp.LsEntry> entryList = sftpChannel.ls(intermediario.getCodIntermediario() + "*_AV_ESITO.zip");
							log.debug("Trovati " + entryList.size() + " tracciati.");

							// Scorro i files e provo ad acquisirli
							for(ChannelSftp.LsEntry entry : entryList) {

								TracciatoFilter filter = tracciatiBD.newFilter();
								filter.setFilenameRichiesta(entry.getFilename());

								if(tracciatiBD.count(filter) == 0) {

									log.debug("Inserimento tracciato di esito avvisatura " + entry.getFilename() + "...");

									Tracciato tracciato = new Tracciato();

									String[] split = entry.getFilename().split("_");
									if(split.length != 6) {
										throw new Exception("Nome file "+entry.getFilename()+" non corrispondente a specifica");
									}


									tracciato.setCodDominio(split[1]);
									tracciato.setTipo(TIPO_TRACCIATO.AV_ESITO);
									tracciato.setStato(STATO_ELABORAZIONE.ELABORAZIONE);
									tracciato.setDataCaricamento(new Date());
									tracciato.setFileNameRichiesta(entry.getFilename());
									tracciato.setFileNameEsito(entry.getFilename().replace("_AV_ESITO.zip", "_AV_ESITO_ACK.zip"));

									it.govpay.core.beans.tracciati.EsitoAvvisatura esitoAvvisatura = new it.govpay.core.beans.tracciati.EsitoAvvisatura();

									esitoAvvisatura.setNumeroEsiti(0);
									esitoAvvisatura.setStepElaborazione("DA_ACQUISIRE");
									esitoAvvisatura.setDataUltimoAggiornamento(new Date());
									esitoAvvisatura.setPercentualeStep(0);

									tracciato.setBeanDati(serializer.getObject(esitoAvvisatura));

									tracciatiBD.insertTracciato(tracciato);

									log.debug("Inserimento tracciato di esito avvisatura " + entry.getFilename() + " completato con successo");
								} else {
									log.debug("Esito avvisatura " + entry.getFilename() + " gia' acquisito con successo");
								}
							}
							sftpChannel.exit();
						} catch (JSchException | SftpException e) {
							log.error("Non e' stato possibile connettersi al server sftp", e);
						} finally {
							if(channelIn != null) channelIn.disconnect();
							if(sessionIn != null) sessionIn.disconnect();
						}

					} else {
						log.trace("Nessun connettore SFTP in lettura configurato per l'intermediario " + intermediario.getCodIntermediario());
					}
				}



				log.trace("Lettura tracciati in elaborazione...");
				TracciatoFilter filter = tracciatiBD.newFilter();
				filter.setTipo(Arrays.asList(TIPO_TRACCIATO.AV_ESITO));
				filter.setStato(STATO_ELABORAZIONE.ELABORAZIONE);
				List<Tracciato> tracciati = tracciatiBD.findAll(filter);

				if(!tracciati.isEmpty()) {
					log.debug("Trovati "+tracciati.size()+" tracciati in elaborazione...");
					EsitiAvvisaturaBD esitiAvvisaturaBD = new EsitiAvvisaturaBD(bd);

					//					XMLInputFactory xif = XMLInputFactory.newInstance();
					//					TransformerFactory tf = TransformerFactory.newInstance(com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl.class.getName(), null );
					//					Transformer t = tf.newTransformer();

					for(Tracciato tracciato: tracciati) {


						it.govpay.core.beans.tracciati.EsitoAvvisatura beanDati = (it.govpay.core.beans.tracciati.EsitoAvvisatura) deserializer.getObject(tracciato.getBeanDati(), it.govpay.core.beans.tracciati.EsitoAvvisatura.class);

						Dominio dominio = AnagraficaManager.getDominio(bd, tracciato.getCodDominio());
						Intermediario intermediario = dominio.getStazione().getIntermediario(bd);
						log.info("Accedo al servizio SFTP per l'intermediario " + intermediario.getCodIntermediario());
						ConnettoreSftp connettore = intermediario.getConnettoreSftp();
						if(connettore == null) {
							log.warn("Connettore SFTP in uscita non definito per l'intermediario "+intermediario.getCodIntermediario() + ", tracciato [id:"+tracciato.getFileNameRichiesta()+", tipo:Avvisatura Digitale, stato:"+ beanDati.getStepElaborazione() +"] non inviato.");
							continue;
						}

						if("DA_ACQUISIRE".equals(beanDati.getStepElaborazione())) {
							log.debug("Tracciato "+tracciato.getId()+" DA_ACQUISIRE...");


							EsitoAvvisaturaFilter filterByIdTracciato = esitiAvvisaturaBD.newFilter();
							filterByIdTracciato.setIdTracciato(tracciato.getId());
							esitiAvvisaturaBD.deleteAll(filterByIdTracciato);

							ZipInputStream zis = null;
							Session sessionIn = null;
							Channel channelIn = null;
							try {
								// Accedo al server sftp e cerco nuovi flussi di rendicontazione
								JSch jsch = new JSch();

								sessionIn = jsch.getSession(connettore.getHttpUserIn(), connettore.getHostIn(), Integer.parseInt(intermediario.getConnettoreSftp().getPortaIn())); 
								sessionIn.setConfig("StrictHostKeyChecking", "no");
								sessionIn.setPassword(connettore.getHttpPasswIn());
								sessionIn.connect();

								channelIn = sessionIn.openChannel("sftp");
								channelIn.connect();
								ChannelSftp sftpChannel = (ChannelSftp) channelIn;
								InputStream inputStream = sftpChannel.get(tracciato.getFileNameRichiesta());
								if(inputStream != null) {
									zis = new ZipInputStream(inputStream);

									zis.getNextEntry();

									//									XMLStreamReader xsr = xif.createXMLStreamReader(zis);
									//									CtEsitoAvvisoDigitale esitoAvvisoDigitale = AvvisaturaUtils.leggiProssimoEsitoAvvisoDigitale(xsr, t);

									List<it.govpay.model.EsitoAvvisatura> esitoAvvisaturaLst = new ArrayList<>();


									List<CtEsitoAvvisoDigitale> leggiListaAvvisiDigitali = AvvisaturaUtils.leggiListaAvvisiDigitali(zis);
									for(CtEsitoAvvisoDigitale esitoAvvisoDigitale: leggiListaAvvisiDigitali) {

										for(CtEsitoAvvisatura ctEsitoAvvisatura: esitoAvvisoDigitale.getEsitoAvvisatura()) {
											EsitoAvvisatura esitoAvvisatura = new EsitoAvvisatura();
											esitoAvvisatura.setCodDominio(esitoAvvisoDigitale.getIdentificativoDominio());
											esitoAvvisatura.setIdentificativoAvvisatura(esitoAvvisoDigitale.getIdentificativoMessaggioRichiesta());

											esitoAvvisatura.setTipoCanale(Integer.parseInt(ctEsitoAvvisatura.getTipoCanaleEsito()));
											esitoAvvisatura.setCodCanale(ctEsitoAvvisatura.getIdentificativoCanale());
											esitoAvvisatura.setData(ctEsitoAvvisatura.getDataEsito());
											esitoAvvisatura.setCodEsito(ctEsitoAvvisatura.getCodiceEsito());
											esitoAvvisatura.setDescrizioneEsito(ctEsitoAvvisatura.getDescrizioneEsito());
											esitoAvvisatura.setIdTracciato(tracciato.getId());

											esitoAvvisaturaLst.add(esitoAvvisatura);

											//											if(esitoAvvisaturaLst.size() > 100) { //TODO batch e definire dimensione nel properties
											//												esitiAvvisaturaBD.insertEsitoAvvisaturaBatch(esitoAvvisaturaLst);
											//												esitoAvvisaturaLst = new ArrayList<>();
											//											}
										}

									}

									//inserisco i record residui
									esitiAvvisaturaBD.insertEsitoAvvisaturaBatch(esitoAvvisaturaLst);

									beanDati.setDataUltimoAggiornamento(new Date());
									beanDati.setStepElaborazione("ACQUISITO");
									beanDati.setCodiceEsitoPresaInCarico(0);

									tracciatiBD.updateBeanDati(tracciato, serializer.getObject(beanDati));


								} else { //non ho trovato il file di esito, non succedera' mai. Aggiorno la data ultimo aggiornamento del bean dati
									beanDati.setDataUltimoAggiornamento(new Date());
									tracciatiBD.updateBeanDati(tracciato, serializer.getObject(beanDati));
								}
							} catch(UnmarshalException e) {
								log.error("Errore durante la lettura del file di esito'"+tracciato.getFileNameEsito()+"':"+ e.getMessage(),e);
								beanDati.setDescrizioneStepElaborazione(e.getMessage());
								beanDati.setCodiceEsitoPresaInCarico(1);
								beanDati.setDescrizioneEsitoPresaInCarico("File in ingresso non ben formato");
								beanDati.setDataUltimoAggiornamento(new Date());
								beanDati.setStepElaborazione("ACQUISITO");
								tracciato.setBeanDati(serializer.getObject(beanDati));
								tracciatiBD.update(tracciato);
							} catch(Exception e) {
								log.error("Errore durante la lettura del file di esito'"+tracciato.getFileNameEsito()+"':"+ e.getMessage(),e);
								beanDati.setDescrizioneStepElaborazione(e.getMessage());
								beanDati.setDataUltimoAggiornamento(new Date());
								tracciatiBD.updateBeanDati(tracciato, serializer.getObject(beanDati));
							} finally {
								try {if(zis !=null) zis.close();} catch(IOException e) {}
								if(channelIn != null) channelIn.disconnect();
								if(sessionIn != null) sessionIn.disconnect();
							}

						} else if("ACQUISITO".equals(beanDati.getStepElaborazione())) {
							log.debug("Tracciato "+tracciato.getId()+" ACQUISITO...");
							Session sessionOut = null;
							Channel channelOut = null;
							ZipOutputStream zipOutputStreamFileRichiesta = null;
							try {
								// Accedo al server sftp e cerco nuovi flussi di rendicontazione
								JSch jsch = new JSch();
								sessionOut = jsch.getSession(connettore.getHttpUserOut(), connettore.getHostOut(), Integer.parseInt(intermediario.getConnettoreSftp().getPortaOut()));
								sessionOut.setConfig("StrictHostKeyChecking", "no");
								sessionOut.setPassword(connettore.getHttpPasswOut());
								sessionOut.connect();

								channelOut = sessionOut.openChannel("sftp");
								channelOut.connect();
								ChannelSftp sftpChannel = (ChannelSftp) channelOut;


								// Cerco file che finiscono per _AV.zip
								log.info("Scrittura tracciato "+tracciato.getFileNameEsito()+" di Avvisatura digitale...");

								OutputStream outputStreamFileRichiesta = sftpChannel.put(tracciato.getFileNameEsito());
								zipOutputStreamFileRichiesta = new ZipOutputStream(outputStreamFileRichiesta);
								zipOutputStreamFileRichiesta.putNextEntry(new ZipEntry(tracciato.getFileNameEsito().replaceAll("zip", "xml")));

								EsitoPresaInCarico esito = new EsitoPresaInCarico();
								esito.setCodiceEsitoPresaInCarico(BigInteger.valueOf(beanDati.getCodiceEsitoPresaInCarico()));
								esito.setDescrizioneEsitoPresaInCarico(beanDati.getDescrizioneEsitoPresaInCarico());
								esito.setIdentificativoFlusso(tracciato.getFileNameEsito().replace(".zip", ""));

								AvvisaturaUtils.scriviEsitoPresaInCarico(esito, zipOutputStreamFileRichiesta);



								zipOutputStreamFileRichiesta.flush();
								zipOutputStreamFileRichiesta.close();

								sftpChannel.exit();
								sessionOut.disconnect();


								log.info("Scrittura tracciato "+tracciato.getFileNameEsito()+" di Avvisatura digitale completata");

								beanDati.setStepElaborazione("ACKATO");
								beanDati.setDataUltimoAggiornamento(new Date());
								beanDati.setPercentualeStep(0);

								tracciato.setStato(beanDati.getCodiceEsitoPresaInCarico() != 0 ? STATO_ELABORAZIONE.SCARTATO: STATO_ELABORAZIONE.COMPLETATO);
								tracciato.setDataCompletamento(new Date());
								tracciato.setBeanDati(serializer.getObject(beanDati));
								tracciatiBD.update(tracciato);
							} catch(Exception e) {
								log.error("Errore durante la scrittura del file di ack'"+tracciato.getFileNameEsito()+"':"+ e.getMessage(),e);
								beanDati.setDescrizioneStepElaborazione(e.getMessage());
								beanDati.setDataUltimoAggiornamento(new Date());
								tracciatiBD.updateBeanDati(tracciato, serializer.getObject(beanDati));
							} finally {
								try {if(zipOutputStreamFileRichiesta !=null) zipOutputStreamFileRichiesta.close();} catch(IOException e) {}
								if(channelOut!= null) channelOut.disconnect();
								if(sessionOut!= null) sessionOut.disconnect();
							}


						}

					}

				} else {
					log.debug("Nessun tracciato in elaborazione trovato");
				}

				log.debug("Batch esito avvisatura digitale completato");


				aggiornaSondaOK(BATCH_ESITO_AVVISATURA_DIGITALE, bd,ctx);

				return "Esito Avvisatura Digitale OK";
			} else {
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			log.error("Esito Avvisatura Digitale Fallita", e);
			try {
				if(bd.isAutoCommit())
					bd.rollback();
				aggiornaSondaKO(BATCH_ESITO_AVVISATURA_DIGITALE, e, bd,ctx);
			} catch (Throwable e1) {
				log.error("Aggiornamento sonda fallito: " + e1.getMessage(),e1);
			}
			return "Esito Avvisatura Digitale#" + e.getMessage();
		} finally {
			BatchManager.stopEsecuzione(bd, BATCH_ESITO_AVVISATURA_DIGITALE);
			if(bd != null) bd.closeConnection();
		}
	}

	public static String avvisaturaDigitale(IContext ctx){
		BasicBD bd = null;
		List<InviaAvvisaturaThread> threads = new ArrayList<>();
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());

			if(BatchManager.startEsecuzione(bd, BATCH_AVVISATURA_DIGITALE)) {
				log.debug("Batch avvisatura digitale");

				SerializationConfig config = new SerializationConfig();
				config.setDf(SimpleDateFormatUtils.newSimpleDateFormatDataOreMinuti());
				config.setIgnoreNullValues(true);

				IDeserializer deserializer = SerializationFactory.getDeserializer(SERIALIZATION_TYPE.JSON_JACKSON, config);
				ISerializer serializer = SerializationFactory.getSerializer(SERIALIZATION_TYPE.JSON_JACKSON, config);

				VersamentiBD versamentiBD = new VersamentiBD(bd);
				TracciatiBD tracciatiBD = new TracciatiBD(bd);
				TracciatoFilter filter = tracciatiBD.newFilter();
				filter.setTipo(Arrays.asList(TIPO_TRACCIATO.AV));
				filter.setStato(STATO_ELABORAZIONE.ELABORAZIONE);
				List<Tracciato> tracciati = tracciatiBD.findAll(filter);

				if(tracciati.size() > 0) {
					for(Tracciato tracciato: tracciati) {
						Avvisatura avvisatura = (Avvisatura) deserializer.getObject(tracciato.getBeanDati(), Avvisatura.class);

						Intermediario intermediario = AnagraficaManager.getIntermediario(bd, avvisatura.getIntermediario());

						log.info("Accedo al servizio SFTP per l'intermediario " + intermediario.getCodIntermediario());
						ConnettoreSftp connettore = intermediario.getConnettoreSftp();

						if(connettore == null) {
							log.warn("Connettore SFTP in uscita non definito per l'intermediario "+intermediario.getCodIntermediario() + ", tracciato [id:"+tracciato.getFileNameRichiesta()+", tipo:Avvisatura Digitale, stato:"+ avvisatura.getStepElaborazione() +"] non inviato.");
							continue;
						}

						if("IN_SPEDIZIONE".equals(avvisatura.getStepElaborazione()) || "ERRORE_SPEDIZIONE".equals(avvisatura.getStepElaborazione())) {
							Session sessionOut = null;
							Channel channelOut = null;
							ZipOutputStream zipOutputStreamFileRichiesta = null;
							try {
								// Accedo al server sftp e cerco nuovi flussi di rendicontazione

								JSch jsch = new JSch();

								sessionOut = jsch.getSession(connettore.getHttpUserOut(), connettore.getHostOut(), Integer.parseInt(intermediario.getConnettoreSftp().getPortaOut()));
								sessionOut.setConfig("StrictHostKeyChecking", "no");
								sessionOut.setPassword(connettore.getHttpPasswOut());
								sessionOut.connect();

								channelOut = sessionOut.openChannel("sftp");
								channelOut.connect();
								ChannelSftp sftpChannel = (ChannelSftp) channelOut;


								// Cerco file che finiscono per _AV.zip
								log.info("Scrittura tracciato "+tracciato.getFileNameRichiesta()+" di Avvisatura digitale...");

								OutputStream outputStreamFileRichiesta = sftpChannel.put(tracciato.getFileNameRichiesta());
								zipOutputStreamFileRichiesta = new ZipOutputStream(outputStreamFileRichiesta);

								VersamentoFilter versamentiFilter = versamentiBD.newFilter();
								versamentiFilter.setIdTracciato(tracciato.getId());
								versamentiFilter.setOffset(0);
								versamentiFilter.setLimit(GovpayConfig.getInstance().getSizePaginaNumeroVersamentiAvvisaturaDigitale());


								long count = versamentiBD.count(versamentiFilter);
								List<it.govpay.bd.model.Versamento> lstVersamenti = versamentiBD.findAll(versamentiFilter);

								zipOutputStreamFileRichiesta.putNextEntry(new ZipEntry(tracciato.getFileNameRichiesta().replaceAll("zip", "xml")));
								AvvisaturaUtils.scriviHeaderTracciatoAvvisatura(zipOutputStreamFileRichiesta);
								while(lstVersamenti!=null && !lstVersamenti.isEmpty()) {

									for(it.govpay.bd.model.Versamento versamento: lstVersamenti) {

										//leggo le info annidate
										versamento.getDominio(bd);

										List<SingoloVersamento> singoliVersamenti = versamento.getSingoliVersamenti(bd);
										for(SingoloVersamento singoloVersamento: singoliVersamenti) {
											singoloVersamento.getIbanAccredito(bd);
											singoloVersamento.getIbanAppoggio(bd);
										}
										AvvisaturaUtils.scriviVersamentoTracciatoAvvisatura(zipOutputStreamFileRichiesta, versamento);
									}


									avvisatura.setStepElaborazione("IN_SPEDIZIONE");
									avvisatura.setDataUltimoAggiornamento(new Date());
									avvisatura.setPercentualeStep(Math.round(((long) versamentiFilter.getOffset() / count) * 100.0f));

									tracciatiBD.updateBeanDati(tracciato, serializer.getObject(avvisatura));
									versamentiFilter.setOffset(versamentiFilter.getOffset() + lstVersamenti.size());
									lstVersamenti = versamentiBD.findAll(versamentiFilter);

								}

								AvvisaturaUtils.scriviTailTracciatoAvvisatura(zipOutputStreamFileRichiesta);

								zipOutputStreamFileRichiesta.flush();
								zipOutputStreamFileRichiesta.close();

								sftpChannel.exit();
								sessionOut.disconnect();


								log.info("Scrittura tracciato "+tracciato.getFileNameRichiesta()+" di Avvisatura digitale completata");

								avvisatura.setStepElaborazione("SPEDITO");
								avvisatura.setDataUltimoAggiornamento(new Date());
								avvisatura.setPercentualeStep(0);

								tracciatiBD.updateBeanDati(tracciato, serializer.getObject(avvisatura));
							} catch(Exception e) {
								log.error("Errore durante la scrittura del file '"+tracciato.getFileNameRichiesta()+"':"+ e.getMessage(),e);
								avvisatura.setStepElaborazione("ERRORE_SPEDIZIONE");
								avvisatura.setDataUltimoAggiornamento(new Date());
								avvisatura.setDescrizioneStepElaborazione(e.getMessage());
								avvisatura.setPercentualeStep(0);

								tracciatiBD.updateBeanDati(tracciato, serializer.getObject(avvisatura));
							} finally {
								if(channelOut != null) channelOut.disconnect();
								if(sessionOut != null) sessionOut.disconnect();
								if(zipOutputStreamFileRichiesta != null) zipOutputStreamFileRichiesta.close();
							}
						} else if("SPEDITO".equals(avvisatura.getStepElaborazione())) {

							ZipInputStream zis = null;
							Session sessionIn = null;
							Channel channelIn = null;

							try {
								// Accedo al server sftp e cerco nuovi flussi di rendicontazione
								JSch jsch = new JSch();

								sessionIn = jsch.getSession(connettore.getHttpUserIn(), connettore.getHostIn(), Integer.parseInt(intermediario.getConnettoreSftp().getPortaIn()));
								sessionIn.setConfig("StrictHostKeyChecking", "no");
								sessionIn.setPassword(connettore.getHttpPasswIn());
								sessionIn.connect();

								channelIn = sessionIn.openChannel("sftp");
								channelIn.connect();
								ChannelSftp sftpChannel = (ChannelSftp) channelIn;
								InputStream inputStream = sftpChannel.get(tracciato.getFileNameEsito());
								if(inputStream != null) {
									zis = new ZipInputStream(inputStream);

									zis.getNextEntry();

									EsitoPresaInCarico esitoPresaInCaricoAvvisoDigitale = AvvisaturaUtils.leggiEsitoPresaInCaricoAvvisoDigitale(zis);

									avvisatura.setStepElaborazione("ACKATO");
									avvisatura.setDescrizioneStepElaborazione(null);
									avvisatura.setPercentualeStep(0);
									avvisatura.setCodiceEsitoPresaInCarico(esitoPresaInCaricoAvvisoDigitale.getCodiceEsitoPresaInCarico().intValue());
									avvisatura.setDescrizioneEsitoPresaInCarico(esitoPresaInCaricoAvvisoDigitale.getDescrizioneEsitoPresaInCarico());
									avvisatura.setDataUltimoAggiornamento(new Date());

									tracciato.setStato(avvisatura.getCodiceEsitoPresaInCarico() != 0 ? STATO_ELABORAZIONE.SCARTATO: STATO_ELABORAZIONE.COMPLETATO);
									tracciato.setDataCompletamento(new Date());
									tracciato.setBeanDati(serializer.getObject(avvisatura));

									tracciatiBD.update(tracciato);
								} else { //non ho trovato il file di ack
									avvisatura.setDataUltimoAggiornamento(new Date());
									tracciatiBD.updateBeanDati(tracciato, serializer.getObject(avvisatura));
								}
							} catch(Exception e) {
								log.warn("Errore durante la lettura del file di ack'"+tracciato.getFileNameEsito()+"':"+ e.getMessage(),e);
								avvisatura.setDescrizioneStepElaborazione(e.getMessage());
								avvisatura.setDataUltimoAggiornamento(new Date());
								tracciatiBD.updateBeanDati(tracciato, serializer.getObject(avvisatura));
							} finally {
								try {if(zis !=null) zis.close();} catch(IOException e) {}
								if(channelIn != null) channelIn.disconnect();
								if(sessionIn != null) sessionIn.disconnect();
							}

						}

					}
				}

				boolean wasAutoCommit = bd.isAutoCommit();

				SimpleDateFormat sdfYYYYMMDD = new SimpleDateFormat("yyyyMMdd"); 
				VersamentoFilter versamentiFilter = versamentiBD.newFilter();
				versamentiFilter.setAvvisaturaDainviare(true);
				versamentiFilter.setAvvisaturaAbilitata(true); 
				versamentiFilter.setTracciatoNull(true);
				versamentiFilter.setModoAvvisatura(ModoAvvisatura.ASICNRONA.getValue());
				versamentiFilter.setDataFine(new Date());
				List<CountPerDominio> countGroupByIdDominio = versamentiBD.countGroupByIdDominio(versamentiFilter);

				if(countGroupByIdDominio.size() > 0) {
					for(CountPerDominio countPerDominio: countGroupByIdDominio) {
						if(countPerDominio.getCount() > 0) { // avvio la procedura solo se ho trovato dei versamenti da avvisare
							Dominio dominio = AnagraficaManager.getDominio(bd, countPerDominio.getIdDominio());
							Intermediario intermediario = dominio.getStazione().getIntermediario(bd);

							if(intermediario.getConnettorePddAvvisatura() != null && intermediario.getConnettorePddAvvisatura().getUrl() != null) {
								// invio dell'avvisatura col connettore SOAP

								versamentiFilter.setCodDominio(dominio.getCodDominio());
								List<it.govpay.bd.model.Versamento> versamenti = versamentiBD.findAll(versamentiFilter); 
								if(versamenti.size() == 0) {
									log.debug("Nessun versamento da avvisare in modalita' asincrona tramite il connettore SOAP.");
									break;
								}

								log.info("Trovati ["+versamenti.size()+"] versamenti da avvisare in modalita' asincrona tramite il connettore SOAP");
								for(it.govpay.bd.model.Versamento versamento: versamenti) {
									InviaAvvisaturaThread sender = new InviaAvvisaturaThread(versamento, ctx.getTransactionId(), versamentiBD,ctx);
									ThreadExecutorManager.getClientPoolExecutorAvvisaturaDigitale().execute(sender);
									threads.add(sender);
								}

							} else if(intermediario.getConnettoreSftp() != null) {
								// invio dell'avvisatura col connettore FTP (avvio creazione del tracciato) 
								try {
									if(wasAutoCommit)
										bd.setAutoCommit(false);
									long countVersamentiTracciati = 0;
									String filenameFormat = intermediario.getCodIntermediario() + "_" + dominio.getCodDominio() + "_" + sdfYYYYMMDD.format(new Date());
									TracciatoFilter tracciatiFilter = tracciatiBD.newFilter();
									tracciatiFilter.setFilenameRichiestaLike(filenameFormat);
									tracciatiFilter.setTipo(Arrays.asList(TIPO_TRACCIATO.AV));
									long countTracciatiPerDominioPergiorno = tracciatiBD.count(tracciatiFilter);

									while(countVersamentiTracciati < countPerDominio.getCount()) {
										long countVersamentiNextTracciato = Math.min(countPerDominio.getCount()-countVersamentiTracciati, GovpayConfig.getInstance().getSizePaginaNumeroVersamentiAvvisaturaDigitale());

										Tracciato tracciato = new Tracciato();
										//populate tracciato
										tracciato.setCodDominio(dominio.getCodDominio());
										tracciato.setTipo(TIPO_TRACCIATO.AV);
										tracciato.setStato(STATO_ELABORAZIONE.ELABORAZIONE);
										tracciato.setDataCaricamento(new Date());
										Avvisatura avvisatura = new Avvisatura();
										avvisatura.setIntermediario(intermediario.getCodIntermediario());
										avvisatura.setNumeroAvvisi(countVersamentiNextTracciato);
										avvisatura.setStepElaborazione("IN_SPEDIZIONE");
										avvisatura.setDataUltimoAggiornamento(new Date());
										avvisatura.setPercentualeStep(0);
										tracciato.setBeanDati(serializer.getObject(avvisatura));
										tracciato.setFileNameRichiesta(filenameFormat + "_" + String.format("%02d", countTracciatiPerDominioPergiorno) + "_" + TIPO_TRACCIATO.AV.toString() + ".zip");
										tracciato.setFileNameEsito(filenameFormat + "_" + String.format("%02d", countTracciatiPerDominioPergiorno) + "_" + TIPO_TRACCIATO.AV.toString() + "_ACK.zip");

										tracciatiBD.insertTracciato(tracciato);
										versamentiBD.updateConLimit(countPerDominio.getIdDominio(), tracciato.getId(), countVersamentiNextTracciato);
										if(!bd.isAutoCommit())
											bd.commit();

										countVersamentiTracciati += countVersamentiNextTracciato;
										countTracciatiPerDominioPergiorno++;
									}
								}catch (Exception e) {
									if(!bd.isAutoCommit()) bd.rollback();
									throw e;
								} finally {
									if(wasAutoCommit)
										bd.setAutoCommit(wasAutoCommit);
								}
							} else {
								log.warn("Spedizione avvisatura digitale per il Dominio ["+dominio.getCodDominio()+"] in modalita' asincrona non avviata: l'intermediario associato al dominio non dispone di un connettore SOAP o FTP valido.");
							}
						}
					}

				}
				log.debug("Batch avvisatura digitale terminato");

				aggiornaSondaOK(BATCH_AVVISATURA_DIGITALE, bd,ctx);

				return "Avvisatura digitale ok";
			} else {
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}

		} catch (Exception e) {
			log.error("Avvisatura digitale Fallita", e);
			try {
				if(!bd.isAutoCommit()) bd.rollback();
				aggiornaSondaKO(BATCH_AVVISATURA_DIGITALE, e, bd,ctx);
			} catch (Throwable e1) {
				log.error("Aggiornamento sonda fallito: " + e1.getMessage(),e1);
			}
			return "Avvisatura digitale#" + e.getMessage();
		} finally {

			// se la lista dei thread contiene elementi controllo che siano tutti terminati prima di uscire.
			if(threads.size() > 0) {
				// Aspetto che abbiano finito tutti
				while(true){
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {

					}
					boolean completed = true;
					for(InviaAvvisaturaThread sender : threads) {
						if(!sender.isCompleted()) 
							completed = false;
					}

					if(!completed) {
						try {
							BatchManager.aggiornaEsecuzione(bd, BATCH_AVVISATURA_DIGITALE);
						} catch (ServiceException e) {
						}  
					} else 
						break;
				}

				log.info("Spedizione avvisatura digitale in modalita' asincrona tramite il connettore SOAP completata.");
			}

			BatchManager.stopEsecuzione(bd, BATCH_AVVISATURA_DIGITALE);
			if(bd != null) bd.closeConnection();
		}
	}

	public static String avvisaturaDigitaleModalitaSincrona(IContext ctx){
		BasicBD bd = null;
		List<InviaAvvisaturaThread> threads = new ArrayList<>();
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());

			if(BatchManager.startEsecuzione(bd, BATCH_AVVISATURA_DIGITALE_SINCRONA)) {
				log.trace("Spedizione Avvisatura Digitale modalita' sincrona");
				VersamentiBD versamentiBD = new VersamentiBD(bd);

				VersamentoFilter versamentiFilter = versamentiBD.newFilter();
				versamentiFilter.setAvvisaturaDainviare(true);
				versamentiFilter.setAvvisaturaAbilitata(true);
				versamentiFilter.setModoAvvisatura(ModoAvvisatura.SINCRONA.getValue());
				versamentiFilter.setDataFine(new Date());
				versamentiFilter.setLimit(100);

				List<it.govpay.bd.model.Versamento> versamenti = versamentiBD.findAll(versamentiFilter); 
				if(versamenti.size() == 0) {
					log.debug("Nessun versamento da avvisare in modalita' sincrona.");
					return "Nessuna versamento da avvisare in modalita' sincrona.";
				}

				log.info("Trovati ["+versamenti.size()+"] versamenti da avvisare in modalita' sincrona");
				for(it.govpay.bd.model.Versamento versamento: versamenti) {
					Intermediario intermediario = versamento.getDominio(bd).getStazione().getIntermediario(bd);

					if(intermediario.getConnettorePddAvvisatura() != null && intermediario.getConnettorePddAvvisatura().getUrl() != null) {
						InviaAvvisaturaThread sender = new InviaAvvisaturaThread(versamento, ctx.getTransactionId(), versamentiBD,ctx);
						ThreadExecutorManager.getClientPoolExecutorAvvisaturaDigitale().execute(sender);
						threads.add(sender);
					} else {
						log.warn("Spedizione avvisatura Versamento [Dominio: "+versamento.getDominio(bd).getCodDominio()+", NumeroAvviso: "+versamento.getNumeroAvviso()+"] in modalita' sincrona non avviata: l'intermediario associato al dominio non dispone di un connettore SOAP valido.");
						versamentiBD.updateVersamentoModalitaAvvisatura(versamento.getId(), ModoAvvisatura.ASICNRONA.getValue());
					}
				}
				log.info("Processi di spedizione avvisatura versamento in modalita' sincrona avviati.");
				aggiornaSondaOK(BATCH_AVVISATURA_DIGITALE_SINCRONA, bd,ctx);
			} else {
				log.info("Operazione in corso su altro nodo. Richiesta interrotta.");
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			log.error("Non è stato possibile avviare la spedizione dell'avvisatura digitale in modalita' sincrona", e);
			aggiornaSondaKO(BATCH_AVVISATURA_DIGITALE_SINCRONA, e, bd,ctx); 
			return "Non è stato possibile avviare la spedizione dell'avvisatura digitale in modalita' sincrona: " + e;
		} finally {
			if(bd != null) bd.closeConnection();
		}

		// Aspetto che abbiano finito tutti
		while(true){
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {

			}
			boolean completed = true;
			for(InviaAvvisaturaThread sender : threads) {
				if(!sender.isCompleted()) 
					completed = false;
			}

			if(completed) {
				try {
					bd = BasicBD.newInstance(ctx.getTransactionId());
					BatchManager.stopEsecuzione(bd, BATCH_AVVISATURA_DIGITALE_SINCRONA);
				} catch (ServiceException e) {
				} finally {
					if(bd != null) bd.closeConnection();
				}
				log.info("Spedizione avvisatura digitale in modalita' sincrona completata.");
				return "Spedizione avvisatura digitale in modalita' sincrona completata.";
			} else {
				try {
					bd = BasicBD.newInstance(ctx.getTransactionId());
					BatchManager.aggiornaEsecuzione(bd, BATCH_AVVISATURA_DIGITALE_SINCRONA);
				} catch (ServiceException e) {
				} finally {
					if(bd != null) bd.closeConnection();
				}
			}
		}
	}

	public static String elaborazioneTracciatiPendenze(IContext ctx){
		BasicBD bd = null;
		boolean wasAutoCommit = false;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());
			wasAutoCommit = bd.isAutoCommit();

			if(BatchManager.startEsecuzione(bd, BATCH_TRACCIATI)) {
				wasAutoCommit = bd.isAutoCommit();
				if(wasAutoCommit)
					bd.setAutoCommit(false);
				log.trace("Elaborazione tracciati");

				TracciatiBD tracciatiBD = new TracciatiBD(bd);
				TracciatoFilter filter = tracciatiBD.newFilter();
				filter.setTipo(Arrays.asList(TIPO_TRACCIATO.PENDENZA));
				filter.setStato(STATO_ELABORAZIONE.ELABORAZIONE);
				filter.setLimit(25);
				//				filter.setDataUltimoAggiornamentoMax(new Date());
				List<Tracciato> tracciati = tracciatiBD.findAll(filter);
				Tracciati tracciatiBusiness = new Tracciati(bd);

				while(!tracciati.isEmpty()) {
					log.info("Trovati ["+tracciati.size()+"] tracciati da elaborare...");

					for(Tracciato tracciato: tracciati) {
						log.info("Avvio elaborazione tracciato "  + tracciato.getId());
						ElaboraTracciatoDTO elaboraTracciatoDTO = new ElaboraTracciatoDTO();
						elaboraTracciatoDTO.setTracciato(tracciato);
						tracciatiBusiness.elaboraTracciatoPendenze(elaboraTracciatoDTO);
						log.info("Elaborazione tracciato "  + tracciato.getId() + " completata");
					}

					//					filter.setDataUltimoAggiornamentoMax(new Date());
					tracciati = tracciatiBD.findAll(filter);
				}

				aggiornaSondaOK(BATCH_TRACCIATI, bd, ctx);
				BatchManager.stopEsecuzione(bd, BATCH_TRACCIATI);
				log.info("Elaborazione tracciati terminata.");
				return "Elaborazione tracciati terminata.";
			} else {
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			try {
				if(!bd.isAutoCommit()) bd.rollback();
				aggiornaSondaKO(BATCH_TRACCIATI, e, bd, ctx);
			} catch (Throwable e1) {
				log.error("Aggiornamento sonda fallito: " + e1.getMessage(),e1);
			}
			log.error("Non è stato possibile eseguire l'elaborazione dei tracciati", e);
			return "Non è stato possibile eseguire l'elaborazione dei tracciati: " + e;
		} finally {
			if(bd != null) bd.closeConnection();
		}
	}

	public static String spedizionePromemoria(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());

			Configurazione configurazioneBD = new Configurazione(bd);
			it.govpay.bd.model.Configurazione configurazione = configurazioneBD.getConfigurazione();
			MailBatch batchSpedizioneEmail = configurazione.getBatchSpedizioneEmail();
			if(!batchSpedizioneEmail.isAbilitato()) {
				return "Spedizione promemoria disabilitata.";
			}

			if(BatchManager.startEsecuzione(bd, BATCH_SPEDIZIONE_PROMEMORIA)) {
				int limit = 100;
				log.trace("Spedizione primi ["+limit+"] promemoria non consegnati");
				Promemoria promemoriaBD = new Promemoria(bd); 
				List<it.govpay.bd.model.Promemoria> promemorias  = promemoriaBD.findPromemoriaDaSpedire(0, limit);

				if(promemorias.size() == 0) {
					aggiornaSondaOK(BATCH_SPEDIZIONE_PROMEMORIA, bd,ctx);
					BatchManager.stopEsecuzione(bd, BATCH_SPEDIZIONE_PROMEMORIA);
					log.debug("Nessun promemoria da inviare.");
					return "Nessun promemoria da inviare.";
				}

				log.info("Trovati ["+promemorias.size()+"] promemoria da spedire");

				for(it.govpay.bd.model.Promemoria promemoria: promemorias) {
					promemoriaBD.invioPromemoria(promemoria);
				}
				aggiornaSondaOK(BATCH_SPEDIZIONE_PROMEMORIA, bd,ctx);
				log.info("Spedizione promemoria completata.");
				return "Spedizione promemoria completata.";
			} else {
				log.info("Operazione in corso su altro nodo. Richiesta interrotta.");
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			log.error("Non è stato possibile avviare la spedizione dei promemoria", e);
			try {
				if(!bd.isAutoCommit()) bd.rollback();
				aggiornaSondaKO(BATCH_SPEDIZIONE_PROMEMORIA, e, bd,ctx); 
			} catch (Throwable e1) {
				log.error("Aggiornamento sonda fallito: " + e1.getMessage(),e1);
			}
			return "Non è stato possibile avviare la spedizione dei promemoria: " + e;
		} finally {
			if(bd != null) bd.closeConnection();
		}
	}
	
	public static String gestionePromemoria(IContext ctx){
		BasicBD bd = null;
		try {
			bd = BasicBD.newInstance(ctx.getTransactionId());

			Configurazione configurazioneBD = new Configurazione(bd);
			it.govpay.bd.model.Configurazione configurazione = configurazioneBD.getConfigurazione();
			MailBatch batchSpedizioneEmail = configurazione.getBatchSpedizioneEmail();
			AppIOBatch batchSpedizioneAppIO = configurazione.getBatchSpedizioneAppIo();
			
			if(!batchSpedizioneEmail.isAbilitato() && !batchSpedizioneAppIO.isAbilitato()) {
				return "Spedizione promemoria Email e AppIO disabilitata.";
			}

			if(BatchManager.startEsecuzione(bd, BATCH_GESTIONE_PROMEMORIA)) {
				int limit = 100;
				
				log.trace("Elaborazione primi ["+limit+"] versamenti con promemoria avviso non consegnati");
				VersamentiBD versamentiBD = new VersamentiBD(bd);
				it.govpay.core.business.Versamento versamentoBusiness = new it.govpay.core.business.Versamento(bd);
				List<Versamento> listaPromemoriaAvviso = versamentiBD.findVersamentiConAvvisoDiPagamentoDaSpedire(0, limit);
				
				if(listaPromemoriaAvviso.size() == 0) {
					log.debug("Nessun promemoria avviso da inviare, controllo presenza promemoria scadenza.");
				} else {
					// elaborazione avvisi...
					log.info("Trovati ["+listaPromemoriaAvviso.size()+"] promemoria avviso da spedire");
					for (Versamento versamento : listaPromemoriaAvviso) {
						versamentoBusiness.inserisciPromemoriaAvviso(versamento);				
					}
				}
				
				aggiornaSondaOK(BATCH_GESTIONE_PROMEMORIA, bd,ctx);

				log.trace("Elaborazione primi ["+limit+"] versamenti con promemoria scadenza via mail non consegnati");
				List<Versamento> listaPromemoriaScadenzaMail = versamentiBD.findVersamentiConAvvisoDiScadenzaDaSpedireViaMail(0, limit);
				
				if(listaPromemoriaScadenzaMail.size() == 0) {
					log.debug("Nessun promemoria scadenza da inviare via mail.");
				} else {
					log.info("Trovati ["+listaPromemoriaScadenzaMail.size()+"] promemoria scadenza da spedire via mail");
					for (Versamento versamento : listaPromemoriaScadenzaMail) {
						versamentoBusiness.inserisciPromemoriaScadenzaMail(versamento);
					}
				}
				
				aggiornaSondaOK(BATCH_GESTIONE_PROMEMORIA, bd,ctx);
				
				log.trace("Elaborazione primi ["+limit+"] versamenti con promemoria scadenza via appIO non consegnati");
				List<Versamento> listaPromemoriaScadenzaAppIO = versamentiBD.findVersamentiConAvvisoDiScadenzaDaSpedireViaAppIO(0, limit);
				
				if(listaPromemoriaScadenzaAppIO.size() == 0) {
					log.debug("Nessun promemoria scadenza da inviare via appIO.");
				} else {
					log.info("Trovati ["+listaPromemoriaScadenzaAppIO.size()+"] promemoria scadenza da spedire");
					for (Versamento versamento : listaPromemoriaScadenzaAppIO) {
						versamentoBusiness.inserisciPromemoriaScadenzaAppIO(versamento);
					}
				}
				
				aggiornaSondaOK(BATCH_GESTIONE_PROMEMORIA, bd,ctx);
				BatchManager.stopEsecuzione(bd, BATCH_GESTIONE_PROMEMORIA);
				log.info("Gestione promemoria completata.");
				return "Gestione promemoria completata.";
			} else {
				log.info("Operazione in corso su altro nodo. Richiesta interrotta.");
				return "Operazione in corso su altro nodo. Richiesta interrotta.";
			}
		} catch (Exception e) {
			log.error("Non è stato possibile avviare la gestione dei promemoria", e);
			try {
				if(!bd.isAutoCommit()) bd.rollback();
				aggiornaSondaKO(BATCH_GESTIONE_PROMEMORIA, e, bd,ctx); 
			} catch (Throwable e1) {
				log.error("Aggiornamento sonda fallito: " + e1.getMessage(),e1);
			}
			return "Non è stato possibile avviare la gestione dei promemoria: " + e;
		} finally {
			if(bd != null) bd.closeConnection();
		}
	}
}
