/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC 
 * http://www.gov4j.it/govpay
 * 
 * Copyright (c) 2014-2016 Link.it srl (http://www.link.it).
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
package it.govpay.orm.dao.jdbc;

import org.openspcoop2.generic_project.exception.NotImplementedException;
import org.openspcoop2.generic_project.exception.ServiceException;

import java.sql.Connection;

import javax.sql.DataSource;

import it.govpay.orm.dao.IDominioServiceSearch;
import it.govpay.orm.dao.IDominioService;
import it.govpay.orm.dao.IPspServiceSearch;
import it.govpay.orm.dao.IPspService;
import it.govpay.orm.dao.ICanaleServiceSearch;
import it.govpay.orm.dao.ICanaleService;
import it.govpay.orm.dao.IUoServiceSearch;
import it.govpay.orm.dao.IUoService;
import it.govpay.orm.dao.IPortaleServiceSearch;
import it.govpay.orm.dao.IPortaleService;
import it.govpay.orm.dao.IConnettoreServiceSearch;
import it.govpay.orm.dao.IConnettoreService;
import it.govpay.orm.dao.IIntermediarioServiceSearch;
import it.govpay.orm.dao.IIntermediarioService;
import it.govpay.orm.dao.IStazioneServiceSearch;
import it.govpay.orm.dao.IStazioneService;
import it.govpay.orm.dao.IApplicazioneServiceSearch;
import it.govpay.orm.dao.IApplicazioneService;
import it.govpay.orm.dao.IIbanAccreditoServiceSearch;
import it.govpay.orm.dao.IIbanAccreditoService;
import it.govpay.orm.dao.ITributoServiceSearch;
import it.govpay.orm.dao.ITributoService;
import it.govpay.orm.dao.IOperatoreServiceSearch;
import it.govpay.orm.dao.IOperatoreService;
import it.govpay.orm.dao.IIUVServiceSearch;
import it.govpay.orm.dao.IIUVService;
import it.govpay.orm.dao.IVersamentoServiceSearch;
import it.govpay.orm.dao.IVersamentoService;
import it.govpay.orm.dao.IEventoServiceSearch;
import it.govpay.orm.dao.IEventoService;
import it.govpay.orm.dao.ISingoloVersamentoServiceSearch;
import it.govpay.orm.dao.ISingoloVersamentoService;
import it.govpay.orm.dao.IRPTServiceSearch;
import it.govpay.orm.dao.IRPTService;
import it.govpay.orm.dao.IRRServiceSearch;
import it.govpay.orm.dao.IRRService;
import it.govpay.orm.dao.IPagamentoServiceSearch;
import it.govpay.orm.dao.IPagamentoService;
import it.govpay.orm.dao.INotificaServiceSearch;
import it.govpay.orm.dao.INotificaService;
import it.govpay.orm.dao.IFRServiceSearch;
import it.govpay.orm.dao.IFRService;
import it.govpay.orm.dao.IFrApplicazioneServiceSearch;
import it.govpay.orm.dao.IFrApplicazioneService;
import it.govpay.orm.dao.IRendicontazioneSenzaRPTServiceSearch;
import it.govpay.orm.dao.IRendicontazioneSenzaRPTService;

/**     
 * Manager that allows you to obtain the services of research and management of objects
 *
 * @author Giovanni Bussu (bussu@link.it)
 * @author Lorenzo Nardi (nardi@link.it)
 * @author $Author$
 * @version $Rev$, $Date$
 */

public class JDBCLimitedServiceManager extends JDBCServiceManager {

	private JDBCServiceManager unlimitedJdbcServiceManager;

	public JDBCLimitedServiceManager(JDBCServiceManager jdbcServiceManager) throws ServiceException {
		this.datasource = jdbcServiceManager.get_Datasource();
		this.connection = jdbcServiceManager.get_Connection();
		this.log = jdbcServiceManager.get_Logger();
		this.jdbcProperties = jdbcServiceManager.get_JdbcProperties();
		this.unlimitedJdbcServiceManager = jdbcServiceManager;
	}
	
	
	@Override
	public Connection getConnection() throws ServiceException {
		throw new ServiceException("Connection managed from framework");
	}
	@Override
	public void closeConnection(Connection connection) throws ServiceException {
		throw new ServiceException("Connection managed from framework");
	}
	@Override
	protected Connection get_Connection() throws ServiceException {
		throw new ServiceException("Connection managed from framework");
	}
	@Override
	protected DataSource get_Datasource() throws ServiceException {
		throw new ServiceException("Connection managed from framework");
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Dominio type:Dominio
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Dominio}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Dominio}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IDominioServiceSearch getDominioServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCDominioServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Dominio}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Dominio}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IDominioService getDominioService() throws ServiceException,NotImplementedException{
		return new JDBCDominioService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Psp type:Psp
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Psp}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Psp}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IPspServiceSearch getPspServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCPspServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Psp}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Psp}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IPspService getPspService() throws ServiceException,NotImplementedException{
		return new JDBCPspService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Canale type:Canale
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Canale}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Canale}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public ICanaleServiceSearch getCanaleServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCCanaleServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Canale}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Canale}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public ICanaleService getCanaleService() throws ServiceException,NotImplementedException{
		return new JDBCCanaleService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Uo type:Uo
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Uo}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Uo}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IUoServiceSearch getUoServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCUoServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Uo}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Uo}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IUoService getUoService() throws ServiceException,NotImplementedException{
		return new JDBCUoService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Portale type:Portale
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Portale}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Portale}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IPortaleServiceSearch getPortaleServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCPortaleServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Portale}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Portale}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IPortaleService getPortaleService() throws ServiceException,NotImplementedException{
		return new JDBCPortaleService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Connettore type:Connettore
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Connettore}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Connettore}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IConnettoreServiceSearch getConnettoreServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCConnettoreServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Connettore}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Connettore}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IConnettoreService getConnettoreService() throws ServiceException,NotImplementedException{
		return new JDBCConnettoreService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Intermediario type:Intermediario
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Intermediario}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Intermediario}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IIntermediarioServiceSearch getIntermediarioServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCIntermediarioServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Intermediario}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Intermediario}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IIntermediarioService getIntermediarioService() throws ServiceException,NotImplementedException{
		return new JDBCIntermediarioService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Stazione type:Stazione
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Stazione}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Stazione}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IStazioneServiceSearch getStazioneServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCStazioneServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Stazione}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Stazione}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IStazioneService getStazioneService() throws ServiceException,NotImplementedException{
		return new JDBCStazioneService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Applicazione type:Applicazione
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Applicazione}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Applicazione}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IApplicazioneServiceSearch getApplicazioneServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCApplicazioneServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Applicazione}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Applicazione}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IApplicazioneService getApplicazioneService() throws ServiceException,NotImplementedException{
		return new JDBCApplicazioneService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:IbanAccredito type:IbanAccredito
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.IbanAccredito}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.IbanAccredito}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IIbanAccreditoServiceSearch getIbanAccreditoServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCIbanAccreditoServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.IbanAccredito}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.IbanAccredito}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IIbanAccreditoService getIbanAccreditoService() throws ServiceException,NotImplementedException{
		return new JDBCIbanAccreditoService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Tributo type:Tributo
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Tributo}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Tributo}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public ITributoServiceSearch getTributoServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCTributoServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Tributo}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Tributo}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public ITributoService getTributoService() throws ServiceException,NotImplementedException{
		return new JDBCTributoService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Operatore type:Operatore
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Operatore}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Operatore}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IOperatoreServiceSearch getOperatoreServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCOperatoreServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Operatore}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Operatore}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IOperatoreService getOperatoreService() throws ServiceException,NotImplementedException{
		return new JDBCOperatoreService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:IUV type:IUV
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.IUV}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.IUV}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IIUVServiceSearch getIUVServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCIUVServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.IUV}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.IUV}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IIUVService getIUVService() throws ServiceException,NotImplementedException{
		return new JDBCIUVService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Versamento type:Versamento
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Versamento}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Versamento}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IVersamentoServiceSearch getVersamentoServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCVersamentoServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Versamento}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Versamento}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IVersamentoService getVersamentoService() throws ServiceException,NotImplementedException{
		return new JDBCVersamentoService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Evento type:Evento
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Evento}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Evento}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IEventoServiceSearch getEventoServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCEventoServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Evento}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Evento}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IEventoService getEventoService() throws ServiceException,NotImplementedException{
		return new JDBCEventoService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:SingoloVersamento type:SingoloVersamento
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.SingoloVersamento}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.SingoloVersamento}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public ISingoloVersamentoServiceSearch getSingoloVersamentoServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCSingoloVersamentoServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.SingoloVersamento}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.SingoloVersamento}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public ISingoloVersamentoService getSingoloVersamentoService() throws ServiceException,NotImplementedException{
		return new JDBCSingoloVersamentoService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:RPT type:RPT
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.RPT}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.RPT}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IRPTServiceSearch getRPTServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCRPTServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.RPT}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.RPT}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IRPTService getRPTService() throws ServiceException,NotImplementedException{
		return new JDBCRPTService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:RR type:RR
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.RR}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.RR}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IRRServiceSearch getRRServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCRRServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.RR}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.RR}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IRRService getRRService() throws ServiceException,NotImplementedException{
		return new JDBCRRService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Pagamento type:Pagamento
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Pagamento}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Pagamento}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IPagamentoServiceSearch getPagamentoServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCPagamentoServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Pagamento}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Pagamento}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IPagamentoService getPagamentoService() throws ServiceException,NotImplementedException{
		return new JDBCPagamentoService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:Notifica type:Notifica
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.Notifica}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.Notifica}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public INotificaServiceSearch getNotificaServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCNotificaServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.Notifica}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.Notifica}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public INotificaService getNotificaService() throws ServiceException,NotImplementedException{
		return new JDBCNotificaService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:FR type:FR
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.FR}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.FR}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IFRServiceSearch getFRServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCFRServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.FR}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.FR}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IFRService getFRService() throws ServiceException,NotImplementedException{
		return new JDBCFRService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:FrApplicazione type:FrApplicazione
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.FrApplicazione}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.FrApplicazione}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IFrApplicazioneServiceSearch getFrApplicazioneServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCFrApplicazioneServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.FrApplicazione}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.FrApplicazione}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IFrApplicazioneService getFrApplicazioneService() throws ServiceException,NotImplementedException{
		return new JDBCFrApplicazioneService(this.unlimitedJdbcServiceManager);
	}
	
	
	
	/*
	 =====================================================================================================================
	 Services relating to the object with name:RendicontazioneSenzaRPT type:RendicontazioneSenzaRPT
	 =====================================================================================================================
	*/
	
	/**
	 * Return a service used to research on the backend on objects of type {@link it.govpay.orm.RendicontazioneSenzaRPT}
	 *
	 * @return Service used to research on the backend on objects of type {@link it.govpay.orm.RendicontazioneSenzaRPT}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IRendicontazioneSenzaRPTServiceSearch getRendicontazioneSenzaRPTServiceSearch() throws ServiceException,NotImplementedException{
		return new JDBCRendicontazioneSenzaRPTServiceSearch(this.unlimitedJdbcServiceManager);
	}
	
	/**
	 * Return a service used to research and manage on the backend on objects of type {@link it.govpay.orm.RendicontazioneSenzaRPT}
	 *
	 * @return Service used to research and manage on the backend on objects of type {@link it.govpay.orm.RendicontazioneSenzaRPT}	
	 * @throws ServiceException Exception thrown when an error occurs during processing of the request
	 * @throws NotImplementedException Exception thrown when the method is not implemented
	 */
	@Override
	public IRendicontazioneSenzaRPTService getRendicontazioneSenzaRPTService() throws ServiceException,NotImplementedException{
		return new JDBCRendicontazioneSenzaRPTService(this.unlimitedJdbcServiceManager);
	}
	
	
	
}