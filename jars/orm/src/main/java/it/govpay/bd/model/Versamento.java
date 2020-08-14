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
package it.govpay.bd.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openspcoop2.generic_project.exception.NotFoundException;
import org.openspcoop2.generic_project.exception.ServiceException;

import it.govpay.bd.BDConfigWrapper;
import it.govpay.bd.BasicBD;
import it.govpay.bd.anagrafica.AnagraficaManager;
import it.govpay.bd.pagamento.DocumentiBD;
import it.govpay.bd.pagamento.IuvBD;
import it.govpay.bd.pagamento.RptBD;
import it.govpay.bd.pagamento.VersamentiBD;
import it.govpay.bd.pagamento.filters.RptFilter;
import it.govpay.model.Iuv;
import it.govpay.model.Iuv.TipoIUV;
import it.govpay.model.TipoVersamento;

public class Versamento extends it.govpay.model.Versamento {
	
	private static final long serialVersionUID = 1L;
	// BUSINESS
	
	private transient List<SingoloVersamento> singoliVersamenti;
	private transient List<Rpt> rpts;
	private transient Applicazione applicazione;
	private transient Dominio dominio;
	private transient UnitaOperativa uo;
	private transient Iuv iuv;
	private transient TipoVersamento tipoVersamento;
	private transient TipoVersamentoDominio tipoVersamentoDominio;
	private transient Documento documento;
	
	// Indica se il versamento e' stato creato o aggiornato. Utile per individuare il codice di ritorno nelle api rest.
	private transient boolean created;
	
	public void addSingoloVersamento(it.govpay.bd.model.SingoloVersamento singoloVersamento) throws ServiceException {
		if(this.singoliVersamenti == null) {
			this.singoliVersamenti = new ArrayList<>();
		}
		
		this.singoliVersamenti.add(singoloVersamento);
		
		if(singoloVersamento.getTipoBollo() != null) {
			this.setBolloTelematico(true);
		}
	}
	
	public List<it.govpay.bd.model.SingoloVersamento> getSingoliVersamenti()  {
		if(this.singoliVersamenti != null)
			Collections.sort(this.singoliVersamenti);
		
		return this.singoliVersamenti;
	}
	
	public List<it.govpay.bd.model.SingoloVersamento> getSingoliVersamenti(BDConfigWrapper configWrapper) throws ServiceException {
		if(this.singoliVersamenti == null && this.getId() != null) {
			VersamentiBD versamentiBD = new VersamentiBD(configWrapper);
			this.singoliVersamenti = versamentiBD.getSingoliVersamenti(this.getId());
		}
		
		if(this.singoliVersamenti != null)
			Collections.sort(this.singoliVersamenti);
		
		return this.singoliVersamenti;
	}
	
	public List<it.govpay.bd.model.SingoloVersamento> getSingoliVersamenti(BasicBD bd) throws ServiceException {
		if(this.singoliVersamenti == null && this.getId() != null) {
			VersamentiBD versamentiBD = new VersamentiBD(bd);
			versamentiBD.setAtomica(false); // connessione deve essere gia' aperta
			this.singoliVersamenti = versamentiBD.getSingoliVersamenti(this.getId());
		}
		
		if(this.singoliVersamenti != null)
			Collections.sort(this.singoliVersamenti);
		
		return this.singoliVersamenti;
	}
	
	public Applicazione getApplicazione(BDConfigWrapper configWrapper) throws ServiceException {
		if(this.applicazione == null) {
			try {
				this.applicazione = AnagraficaManager.getApplicazione(configWrapper, this.getIdApplicazione());
			} catch (NotFoundException e) {
			}
		} 
		return this.applicazione;
	}
	
	public void setApplicazione(String codApplicazione, BDConfigWrapper configWrapper) throws ServiceException, NotFoundException {
		this.applicazione = AnagraficaManager.getApplicazione(configWrapper, codApplicazione);
		this.setIdApplicazione(this.applicazione.getId());
	}

	public UnitaOperativa getUo(BDConfigWrapper configWrapper) throws ServiceException {
		if(this.getIdUo() != null && this.uo == null) {
			try {
				this.uo = AnagraficaManager.getUnitaOperativa(configWrapper, this.getIdUo());
			} catch (NotFoundException e) {
			}
		}
		return this.uo;
	}

	public Dominio getDominio(BDConfigWrapper configWrapper) throws ServiceException {
		if(this.dominio == null) {
			try {
				this.dominio = AnagraficaManager.getDominio(configWrapper, this.getIdDominio());
			} catch (NotFoundException e) {
			}
		} 
		return this.dominio;
	}
	
	public UnitaOperativa setUo(long idDominio, String codUo, BDConfigWrapper configWrapper) throws ServiceException, NotFoundException {
		this.uo = AnagraficaManager.getUnitaOperativa(configWrapper, idDominio, codUo);
		this.setIdUo(this.uo.getId());
		return this.uo;
	}
	
	public List<Rpt> getRpt(BasicBD bd) throws ServiceException {
		if(this.rpts == null && bd != null) {
			RptBD rptBD = new RptBD(bd);
			RptFilter filter = rptBD.newFilter();
			filter.setIdVersamento(this.getId());
			this.rpts = rptBD.findAll(filter);
		}
		return this.rpts;
	}
	
	public Iuv getIuv(BasicBD bd) throws ServiceException {
		if(this.iuv == null) {
			IuvBD iuvBD = new IuvBD(bd);
			try {
				this.iuv = iuvBD.getIuv(this.getIdApplicazione(), this.getCodVersamentoEnte(), TipoIUV.NUMERICO);
			} catch (NotFoundException e) {
				// Iuv non assegnato.
			}
		}
		return this.iuv;
	}
	
	public TipoVersamento getTipoVersamento(BDConfigWrapper configWrapper) throws ServiceException {
		if(this.tipoVersamento == null) {
			try {
				this.tipoVersamento = AnagraficaManager.getTipoVersamento(configWrapper, this.getIdTipoVersamento());
			} catch (NotFoundException e) {
			}
		} 
		return this.tipoVersamento;
	}
	
	
	public TipoVersamentoDominio getTipoVersamentoDominio(BDConfigWrapper configWrapper) throws ServiceException {
		if(this.tipoVersamentoDominio == null) {
			try {
				this.tipoVersamentoDominio = AnagraficaManager.getTipoVersamentoDominio(configWrapper, this.getIdTipoVersamentoDominio());
			} catch (NotFoundException e) {
			}
		} 
		return this.tipoVersamentoDominio;
	}	
	
	public void setDocumento(Documento documento) {
		this.documento = documento;
	}
	
	public Documento getDocumento() {
		return this.documento;
	}

	public Documento getDocumento(BasicBD bd) throws ServiceException {
		if(this.getIdDocumento() != null && bd != null && this.documento == null) {
			DocumentiBD documentiBD = new DocumentiBD(bd);
			documentiBD.setAtomica(false); // la connessione deve essere gia' aperta
			try {
				this.documento = documentiBD.getDocumento(this.getIdDocumento());
			} catch (NotFoundException e) {
			}
		} 
		return this.documento;
	}

	public boolean isCreated() {
		return created;
	}

	public void setCreated(boolean created) {
		this.created = created;
	}
}
