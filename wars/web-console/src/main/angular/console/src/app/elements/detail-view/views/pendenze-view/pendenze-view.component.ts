import { Component, Input, OnInit, ViewEncapsulation } from '@angular/core';

import { GovpayService } from '../../../../services/govpay.service';
import { UtilService } from '../../../../services/util.service';
import { Voce } from '../../../../services/voce.service';

import { Riepilogo } from '../../../../classes/view/riepilogo';
import { Dato } from '../../../../classes/view/dato';
import { Parameters } from '../../../../classes/parameters';
import { IModalDialog } from '../../../../classes/interfaces/IModalDialog';
import { IExport } from '../../../../classes/interfaces/IExport';

import * as moment from 'moment';
import { ModalBehavior } from '../../../../classes/modal-behavior';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { CronoCode } from '../../../../classes/view/crono-code';
import { StandardCollapse } from '../../../../classes/view/standard-collapse';
import { TwoColsCollapse } from '../../../../classes/view/two-cols-collapse';

declare let JSZip: any;
declare let FileSaver: any;

@Component({
  selector: 'link-pendenze',
  templateUrl: './pendenze-view.component.html',
  styleUrls: ['./pendenze-view.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class PendenzeViewComponent implements IModalDialog, IExport, OnInit {

  @Input() tentativi = [];
  @Input() importi = [];
  @Input() informazioni = [];
  @Input() eventi = [];

  @Input() json: any;
  @Input() modified: boolean = false;


  protected NOTA = UtilService.NOTA;
  protected ADD = UtilService.PATCH_METHODS.ADD;
  protected info: Riepilogo;
  protected infoVisualizzazione: any = { visible: false, titolo: '', campi: [] };
  protected _paymentsSum: number = 0;
  protected _importiOverIcons: string[] = ['file_download'];
  protected _tentativiOverIcons: string[] = ['file_download'];

  protected _isLoadingMore: boolean = false;
  protected _pageRef: any = { next: null, limit: null };
  protected _pageRefTentativi: any = { next: null, limit: null };

  constructor(public gps: GovpayService, public us: UtilService) {
  }

  ngOnInit() {
    this.dettaglioPendenza();
    this.elencoTentativi();
    this.elencoEventi();
  }

  protected dettaglioPendenza(patch: boolean = false) {
    // /pendenze/idA2A/idPendenza
    let _url = UtilService.URL_PENDENZE+'/'+this.json.idA2A+'/'+this.json.idPendenza;
    this.gps.getDataService(_url).subscribe(
      function (_response) {
        let _json = _response.body;
        this.mapJsonDetail(_json);
        this.gps.updateSpinner(false);
        (patch)?this.us.alert('Aggiornamento stato completato'):null;
      }.bind(this),
      (error) => {
        this.gps.updateSpinner(false);
        this.us.onError(error);
      });
  }

  protected mapJsonDetail(_json: any) {
    //Riepilogo
    this.info = new Riepilogo({
      titolo: new Dato({ label: Voce.DESCRIZIONE, value: _json.causale }),
      sottotitolo: new Dato({ label: Voce.DEBITORE, value: Dato.concatStrings([_json.soggettoPagatore.anagrafica.toUpperCase(), _json.soggettoPagatore.identificativo.toUpperCase()], ', ') }),
      importo: this.us.currencyFormat(_json.importo),
      stato: UtilService.STATI_PENDENZE[_json.stato],
      extraInfo: []
    });
    if(_json.dominio.ragioneSociale && _json.dominio.idDominio) {
      this.info.extraInfo.push({label: Voce.ENTE_CREDITORE + ': ', value: Dato.concatStrings([_json.dominio.ragioneSociale, _json.dominio.idDominio], ', ')});
    }
    if(_json.unitaOperativa && _json.unitaOperativa.ragionesociale && _json.unitaOperativa.idUnita) {
      const _uo: string = Dato.concatStrings([_json.unitaOperativa.ragionesociale, _json.unitaOperativa.idUnita], ', ');
      this.info.extraInfo.push({label: Voce.UNITA_OPERATIVA + ': ', value: _uo});
    }
    if(_json.direzione) {
      this.info.extraInfo.push({label: Voce.DIREZIONE + ': ', value: _json.direzione});
    }
    if(_json.divisione) {
      this.info.extraInfo.push({label: Voce.DIVISIONE + ': ', value: _json.divisione});
    }
    if(_json.tipoPendenza && _json.tipoPendenza.descrizione) {
      this.info.extraInfo.push({label: Voce.TIPO_PENDENZA + ': ', value: _json.tipoPendenza.descrizione});
    }
    if(_json.tassonomiaAvviso) {
      this.info.extraInfo.push({ label: Voce.TASSONOMIA_AVVISO+': ', value: _json.tassonomiaAvviso });
    }
    if(_json.tassonomia) {
      this.info.extraInfo.push({ label: Voce.TASSONOMIA_ENTE+': ', value: _json.tassonomia });
    }
    if(_json.annoRiferimento) {
      this.info.extraInfo.push({ label: Voce.ANNO_RIFERIMENTO+': ', value: _json.annoRiferimento });
    }
    if(_json.cartellaPagamento) {
      this.info.extraInfo.push({ label: Voce.CARTELLA_DI_PAGAMENTO+': ', value: _json.cartellaPagamento });
    }
    const _iuv = (_json.iuvAvviso)?_json.iuvAvviso:_json.iuvPagamento;
    if(_iuv) {
      this.info.extraInfo.push({label: Voce.IUV + ': ', value: _iuv});
    }
    if(_json.numeroAvviso) {
      this.info.extraInfo.push({ label: Voce.AVVISO+': ', value: _json.numeroAvviso });
    }
    if(_json.idA2A) {
      this.info.extraInfo.push({ label: Voce.ID_A2A+': ', value: _json.idA2A });
    }
    if(_json.idPendenza) {
      this.info.extraInfo.push({ label: Voce.ID_PENDENZA+': ', value: _json.idPendenza });
    }
    if(_json.dataCaricamento) {
      this.info.extraInfo.push({ label: Voce.DATA_CARICAMENTO+': ', value: moment(_json.dataCaricamento).format('DD/MM/YYYY') });
    }
    if(_json.dataValidita) {
      this.info.extraInfo.push({ label: Voce.VALIDITA+': ', value: moment(_json.dataValidita).format('DD/MM/YYYY') });
    }
    if(_json.dataScadenza) {
      this.info.extraInfo.push({ label: Voce.SCADENZA+': ', value: moment(_json.dataScadenza).format('DD/MM/YYYY') });
    }
    if(_json.dataUltimoAggiornamento) {
      this.info.extraInfo.push({ label: Voce.DATA_ULTIMO_AGGIORNAMENTO+': ', value: moment(_json.dataUltimoAggiornamento).format('DD/MM/YYYY') });
    }

    //Json Visualizzazione
    if(_json.tipoPendenza && _json.tipoPendenza.visualizzazione) {
      try {
        const _vis = JSON.parse(decodeURIComponent(atob(_json.tipoPendenza.visualizzazione).split('').map(function(c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join('')));
        this.infoVisualizzazione.visible = !!(_vis.vistaDettaglio.titolo && _vis.vistaDettaglio.campi.length);
        if (_vis.vistaDettaglio) {
          this.infoVisualizzazione.titolo = _vis.vistaDettaglio.titolo || '';
          this.infoVisualizzazione.campi = _vis.vistaDettaglio.campi.map((field) => {
            return new Dato({label: field.label, value: this.us.searchPropertyByPathString(field.path, _json)});
          });
        }
      } catch (e) {
        console.warn(e);
      }
    }
    //Dettaglio importi
    this._paymentsSum = 0;
    this.importi = _json.voci.map(function(item) {
      let _std = new StandardCollapse();
      _std.titolo = new Dato({ value: item.descrizione });
      _std.elenco = [];
      if(item.tipoBollo) {
        _std.sottotitolo = Dato.arraysToDato([Voce.ID_PENDENZA, Voce.ID_BOLLO], [item.idVocePendenza, item.tipoBollo], ', ');
      } else {
        _std.sottotitolo = new Dato({ label: Voce.ID_PENDENZA+': ', value: item.idVocePendenza });
        _std.elenco.push({ label: Voce.CONTABILITA, value: Dato.concatStrings([ item.tipoContabilita, item.codiceContabilita ], ', ') });
        _std.elenco.push({ label: Voce.CONTO_ACCREDITO, value: item.ibanAccredito });
      }
      _std.importo = this.us.currencyFormat(item.importo);
      _std.stato = item.stato;
      this._paymentsSum += UtilService.defaultDisplay({ value: item.importo, text: 0 });
      let p = new Parameters();
      p.jsonP = item;
      p.model = _std;
      p.type = UtilService.STANDARD_COLLAPSE;
      return p;
    }, this);
    //Note
    if(_json.note) {
      this.informazioni = _json.note.map(function(_nota) {
        let _cc = new CronoCode();
        _cc.data = _nota.data?moment(_nota.data).format('DD/MM/YYYY'):Voce.NON_PRESENTE;
        _cc.codice = _nota.autore;
        _cc.titolo = new Dato({ value: _nota.oggetto });
        _cc.sottotitolo = new Dato({ value: _nota.testo });
        let p = new Parameters();
        p.jsonP = _nota;
        p.model = _cc;
        p.type = UtilService.CRONO_CODE;
        return p;
      }, this);
    }
  }

  protected elencoTentativi() {
    this.gps.getDataService(this.json.rpp).subscribe(function (_response) {
        let _body = _response.body;
        this._pageRefTentativi = { next: (_body['prossimiRisultati'] || null), limit: _body['numPagine']*_body['risultatiPerPagina'] };
        this.tentativi = _body['risultati'].map(function(item) {
          let _date = item.rpt.dataOraMessaggioRichiesta?moment(item.rpt.dataOraMessaggioRichiesta).format('DD/MM/YYYY'):Voce.NON_PRESENTE;
          let _subtitle = Dato.concatStrings([ Voce.DATA+': '+_date, Voce.CCP+': '+item.rpt.datiVersamento.codiceContestoPagamento ], ', ');
          let _std = new StandardCollapse();
          let _map = this._mapStato(item);
          _std.titolo = new Dato({ label: '', value: (item.rt && item.rt.istitutoAttestante)?item.rt.istitutoAttestante.denominazioneAttestante:Voce.NO_PSP });
          _std.sottotitolo = new Dato({ label: '', value: _subtitle });
          _std.stato = _map.stato;
          _std.motivo = _map.motivo;
          let p = new Parameters();
          p.model = _std;
          p.jsonP = item;
          p.type = UtilService.STANDARD_COLLAPSE;
          return p;
        }, this);
        this.gps.updateSpinner(false);
      }.bind(this),
      (error) => {
        this.gps.updateSpinner(false);
        this.us.onError(error);
      });
  }

  protected elencoEventi() {
    let _url = UtilService.URL_GIORNALE_EVENTI;
    let _query = 'idA2A='+this.json.idA2A+'&idPendenza='+this.json.idPendenza;
    this.__getEventi(_url, _query);
  }

  protected __getEventi(_url, _query, _pages = false) {
    if(!this._isLoadingMore) {
      this._isLoadingMore = true;
      this.gps.getDataService(_url, _query).subscribe(function (_response) {
          let _body = _response.body;
          const _evts = _body['risultati'].map(function(item) {
            const _stdTCC: TwoColsCollapse = new TwoColsCollapse();
            const _dataOraEventi = item.dataEvento?moment(item.dataEvento).format('DD/MM/YYYY [-] HH:mm:ss.SSS'):Voce.NON_PRESENTE;
            const _riferimento = this.us.mapRiferimentoGiornale(item);
            _stdTCC.titolo = new Dato({ label: this.us.mappaturaTipoEvento(item.tipoEvento) });
            _stdTCC.sottotitolo = new Dato({ label: _riferimento });
            _stdTCC.stato = item.esito;
            _stdTCC.data = _dataOraEventi;
            if(item.dettaglioEsito) {
              _stdTCC.motivo = item.dettaglioEsito;
            }
            _stdTCC.url = UtilService.RootByTOA() + _url + '/' + item.id;
            _stdTCC.elenco = [];
            if(item.durataEvento) {
              _stdTCC.elenco.push({ label: Voce.DURATA, value: this.us.formatMs(item.durataEvento) });
            }
            if(item.datiPagoPA) {
              if(item.datiPagoPA.idPsp) {
                _stdTCC.elenco.push({ label: Voce.ID_PSP, value: item.datiPagoPA.idPsp });
              }
              if(item.datiPagoPA.idCanale) {
                _stdTCC.elenco.push({ label: Voce.ID_CANALE, value: item.datiPagoPA.idCanale });
              }
              if(item.datiPagoPA.idIntermediarioPsp) {
                _stdTCC.elenco.push({ label: Voce.ID_INTERMEDIARIO_PSP, value: item.datiPagoPA.idIntermediarioPsp });
              }
              if(item.datiPagoPA.tipoVersamento) {
                _stdTCC.elenco.push({ label: Voce.TIPO_VERSAMENTO, value: item.datiPagoPA.tipoVersamento });
              }
              if(item.datiPagoPA.modelloPagamento) {
                _stdTCC.elenco.push({ label: Voce.MODELLO_PAGAMENTO, value: item.datiPagoPA.modelloPagamento });
              }
              if(item.datiPagoPA.idDominio) {
                _stdTCC.elenco.push({ label: Voce.ID_DOMINIO, value: item.datiPagoPA.idDominio });
              }
              if(item.datiPagoPA.idIntermediario) {
                _stdTCC.elenco.push({ label: Voce.ID_INTERMEDIARIO, value: item.datiPagoPA.idIntermediario });
              }
              if(item.datiPagoPA.idStazione) {
                _stdTCC.elenco.push({ label: Voce.ID_STAZIONE, value: item.datiPagoPA.idStazione });
              }
            }
            let p = new Parameters();
            p.model = _stdTCC;
            p.type = UtilService.TWO_COLS_COLLAPSE;
            return p;
          }, this);
          this._pageRef = { next: (_body['prossimiRisultati'] || null), limit: _body['numPagine']*_body['risultatiPerPagina'] };
          this.eventi = _pages?this.eventi.concat(_evts):_evts;
          this._isLoadingMore = false;
          this.gps.updateSpinner(false);
        }.bind(this),
        (error) => {
          this._isLoadingMore = false;
          this.gps.updateSpinner(false);
          this.us.onError(error);
        });
    }
  }

  protected _mapStato(item: any): any {
    let _map: any = { stato: '', motivo: '', codiceEsito: -1 };
    switch (item.stato) {
      case 'RT_ACCETTATA_PA':
        _map.stato = (item.rt)?UtilService.STATI_ESITO_PAGAMENTO[item.rt.datiPagamento.codiceEsitoPagamento]:'n/a';
        _map.codiceEsito = parseInt(item.rt.datiPagamento.codiceEsitoPagamento) || -1;
        break;
      case 'RPT_RIFIUTATA_NODO':
      case 'RPT_RIFIUTATA_PSP':
      case 'RPT_ERRORE_INVIO_A_PSP':
        _map.stato = UtilService.STATI_RPP.FALLITO;
        _map.motivo = item.dettaglioStato+' - stato: '+item.stato;
        break;
      case 'RT_RIFIUTATA_PA':
      case 'RT_ESITO_SCONOSCIUTO_PA':
        _map.stato = UtilService.STATI_RPP.ANOMALO;
        _map.motivo = item.dettaglioStato+' - stato: '+item.stato;
        break;
      default:
        _map.stato = UtilService.STATI_RPP.IN_CORSO;
    }
    return _map;
  }

  protected _addEdit(type: string, patchOperation: string, mode: boolean = false, _viewModel?: any) {
    let _mb: ModalBehavior = new ModalBehavior();
    _mb.editMode = mode;
    _mb.closure = this.refresh.bind(this);
    switch(type) {
      case UtilService.NOTA:
        _mb.async_callback = this.save.bind(this);
        _mb.operation = patchOperation;
        _mb.info.dialogTitle = 'Nuova nota';
        _mb.info.viewModel = this.json;
        _mb.info.templateName = UtilService.NOTA;
        break;
    }
    UtilService.dialogBehavior.next(_mb);
  }

  protected _actionMenuRules(): boolean {
    return (this.tentativi.filter((item : any) => {
      // Filtro per pagamento non eseguito
      return (this._mapStato(item.jsonP).codiceEsito === 1 && item.jsonP.stato === 'RT_ACCETTATA_PA');
    }).length !== 0);
  }

  protected _actionMenuOp(operation: string) {
    let _mb: ModalBehavior = new ModalBehavior();
    _mb.editMode = true;
    _mb.closure = this.refresh.bind(this);
    if (operation === 'sostituisci-rt') {
      _mb.async_callback = this.save.bind(this);
      _mb.operation = UtilService.PATCH_METHODS.REPLACE;
      _mb.info.dialogTitle = 'Sostituzione RT';
      _mb.info.parent = this.tentativi.map((el: any) => {
        return (el.jsonP.rt && el.jsonP.rt.datiPagamento && el.jsonP.rt.datiPagamento.identificativoUnivocoVersamento)?el.jsonP.rt.datiPagamento.identificativoUnivocoVersamento:'';
      }).filter(s => s !== '');
      _mb.info.templateName = UtilService.TENTATIVO_RT;

      UtilService.dialogBehavior.next(_mb);
    }
  }

  protected refreshAfterPatch() {
    this.dettaglioPendenza(true);
    this.elencoTentativi();
    this.elencoEventi();
  }

  protected _loadMoreEventi() {
    if (this._pageRef.next) {
      this.__getEventi(this._pageRef.next, '', true);
    }
  }

  infoDetail(): any {
    return this.json;
  }

  title(): string {
    return UtilService.defaultDisplay({ value: this.json?this.json.causale:null });
  }

  refresh(mb: ModalBehavior) {
    this.modified = false;
    if(mb && mb.info && mb.info.viewModel) {
      switch(mb.info.templateName) {
        case UtilService.TENTATIVO_RT:
          this.refreshAfterPatch();
          break;
        case UtilService.NOTA:
          this.json = mb.info.viewModel;
          this.mapJsonDetail(this.json);
          break;
        default:
          let _service = UtilService.URL_PENDENZE+'/'+this.json.idA2A+'/'+this.json.idPendenza;
          let _json = [
            { op: mb.operation, path: '/stato', value: mb.info.viewModel.stato },
            { op: mb.operation, path: '/descrizioneStato', value: mb.info.viewModel.descrizioneStato }
          ];
          this.gps.saveData(_service, _json, null, UtilService.METHODS.PATCH).subscribe(
            (response) => {
              if(mb.editMode && mb.info.templateName == UtilService.PENDENZA) {
                this.json = response.body;
                this.modified = true;
                this.mapJsonDetail(response.body);
              }
              this.gps.updateSpinner(false);
            },
            (error) => {
              this.gps.updateSpinner(false);
              this.us.onError(error);
            });
          break;
      }
    }
  }

  save(responseService: BehaviorSubject<any>, mb: ModalBehavior) {
    if(mb && mb.info.viewModel) {
      let _json;
      let _query = null;
      let _ref = null;
      let _service = null;
      let _method = null;
      switch(mb.info.templateName) {
        case UtilService.NOTA:
          _ref = UtilService.EncodeURIComponent(this.json.idA2A)+'/'+UtilService.EncodeURIComponent(this.json.idPendenza);
          _service = UtilService.URL_PENDENZE+'/'+_ref;
          _method = UtilService.METHODS.PATCH;
          _json = [{ op: mb.operation, path: '/'+UtilService.NOTA, value: mb.info.viewModel }];
          break;
        case UtilService.TENTATIVO_RT:
          _ref = '/'+UtilService.EncodeURIComponent(mb.info.viewModel.idDominio)+'/'+UtilService.EncodeURIComponent(mb.info.viewModel.codiceIUV)+'/'+UtilService.EncodeURIComponent(mb.info.viewModel.ccp);
          _service = UtilService.URL_RPPS+_ref;
          _method = UtilService.METHODS.PATCH;
          _json = [{ op: mb.operation, path: '/rt', value: mb.info.viewModel.b64 }];
          break;
      }
      if (_service) {
        this.gps.saveData(_service, _json, _query, _method).subscribe(
          (response) => {
            if(mb.editMode) {
              switch (mb.info.templateName) {
                case UtilService.NOTA:
                  mb.info.viewModel = response.body;
                  break;
              }
            }
            this.gps.updateSpinner(false);
            responseService.next(true);
          },
          (error) => {
            this.gps.updateSpinner(false);
            this.us.onError(error);
          });
      }
    }
  }

  esclusioneNotifiche() { }

  exportData() {
    this.gps.updateSpinner(true);
    let _folder = '';
    let urls: string[] = [];
    let contents: string[] = [];
    let types: string[] = [];
    let folders: string[] = [];
    let names: string[] = [];
    try {
      //Pdf Avviso di pagamento
      if(this.json.numeroAvviso) {
        if (folders.indexOf(UtilService.ROOT_ZIP_FOLDER) == -1) {
          folders.push(UtilService.ROOT_ZIP_FOLDER);
        }
        urls.push(UtilService.URL_AVVISI+'/'+UtilService.EncodeURIComponent(this.json.dominio.idDominio)+'/'+UtilService.EncodeURIComponent(this.json.numeroAvviso));
        contents.push('application/pdf');
        names.push(this.json.dominio.idDominio + '_' + this.json.numeroAvviso + '.pdf' + UtilService.ROOT_ZIP_FOLDER);
        types.push('blob');
      }
      this.tentativi.forEach((el) => {
        // /rpp/{idDominio}/{iuv}/{ccp}/rpt
        // /rpp/{idDominio}/{iuv}/{ccp}/rt
        // /eventi/?{idDominio}&{iuv}&{ccp}
        let item = el.jsonP;
        _folder = UtilService.EncodeURIComponent(item.rpt.dominio.identificativoDominio)+'_'+UtilService.EncodeURIComponent(item.rpt.datiVersamento.identificativoUnivocoVersamento)+'_'+UtilService.EncodeURIComponent(item.rpt.datiVersamento.codiceContestoPagamento);
        if (folders.indexOf(_folder) == -1) {
          folders.push(_folder);
        }
        urls.push('/rpp/'+UtilService.EncodeURIComponent(item.rpt.dominio.identificativoDominio)+'/'+UtilService.EncodeURIComponent(item.rpt.datiVersamento.identificativoUnivocoVersamento)+'/'+UtilService.EncodeURIComponent(item.rpt.datiVersamento.codiceContestoPagamento)+'/rpt');
        names.push('Rpt.xml'+_folder);
        contents.push('application/xml');
        types.push('text');
        urls.push(UtilService.URL_GIORNALE_EVENTI+'?risultatiPerPagina='+this._pageRef.limit+'&idDominio='+UtilService.EncodeURIComponent(item.rpt.dominio.identificativoDominio)+'&iuv='+UtilService.EncodeURIComponent(item.rpt.datiVersamento.identificativoUnivocoVersamento)+'&ccp='+UtilService.EncodeURIComponent(item.rpt.datiVersamento.codiceContestoPagamento));
        contents.push('application/json');
        names.push('Eventi.csv'+_folder);
        types.push('json');
        if(item.rt) {
          urls.push('/rpp/'+UtilService.EncodeURIComponent(item.rt.dominio.identificativoDominio)+'/'+UtilService.EncodeURIComponent(item.rt.datiPagamento.identificativoUnivocoVersamento)+'/'+UtilService.EncodeURIComponent(item.rt.datiPagamento.CodiceContestoPagamento)+'/rt');
          contents.push('application/xml');
          names.push('Rt.xml'+_folder);
          types.push('text');
          urls.push('/rpp/'+UtilService.EncodeURIComponent(item.rt.dominio.identificativoDominio)+'/'+UtilService.EncodeURIComponent(item.rt.datiPagamento.identificativoUnivocoVersamento)+'/'+UtilService.EncodeURIComponent(item.rt.datiPagamento.CodiceContestoPagamento)+'/rt');
          contents.push('application/pdf');
          names.push('Rt.pdf'+_folder);
          types.push('blob');
        }
      }, this);
      if (folders.indexOf(UtilService.ROOT_ZIP_FOLDER) == -1) {
        folders.push(UtilService.ROOT_ZIP_FOLDER);
      }
      urls.push(UtilService.URL_GIORNALE_EVENTI+'?risultatiPerPagina='+this._pageRef.limit+'&idA2A='+UtilService.EncodeURIComponent(this.json.idA2A)+'&idPendenza='+UtilService.EncodeURIComponent(this.json.idPendenza));
      contents.push('application/json');
      names.push('Eventi.csv' + UtilService.ROOT_ZIP_FOLDER);
      types.push('json');
    } catch (error) {
      this.gps.updateSpinner(false);
      this.us.alert('Si è verificato un errore non previsto durante il recupero delle informazioni.', true);
    }
    if(urls.length != 0) {
      this.gps.multiExportService(urls, contents, types).subscribe(function (_response) {
          this.saveFile(_response, { folders: folders, names: names }, '.zip');
        }.bind(this),
        (error) => {
          this.gps.updateSpinner(false);
          this.us.onError(error);
        });
    } else {
      this.gps.updateSpinner(false);
      this.us.alert('Nessuna informazione disponibile per eseguire lo scaricamento del resoconto.', true);
    }
  }

  saveFile(data: any, structure: any, ext: string) {
    let root = 'Pendenza_' + this.json.idA2A + '_' + this.json.idPendenza;
    let zipname = root + ext;
    let zip = new JSZip();
    let zroot = zip.folder(root);
    structure.folders.forEach((folder) => {
      let zfolder;
      if(folder !== UtilService.ROOT_ZIP_FOLDER) {
        zfolder = zroot.folder(folder);
      }
      data.forEach((file, ref) => {
        let o;
        if (folder != UtilService.ROOT_ZIP_FOLDER) {
          if (structure.names[ref].indexOf(folder) != -1) {
            //folder
            o = this._elaborate(structure.names[ref].split(folder)[0], file);
            zfolder.file(o['name'], o['zdata']);
            if(o['name'].indexOf('csv') != -1) {
              o = this.createJsonCopy(o['name'], file);
              zfolder.file(o['name'], o['zdata']);
            }
          }
        } else {
          if(structure.names[ref].indexOf(UtilService.ROOT_ZIP_FOLDER) != -1) {
            //root
            o = this._elaborate(structure.names[ref].split(folder)[0], file);
            zroot.file(o['name'], o['zdata']);
            if(o['name'].indexOf('csv') != -1) {
              o = this.createJsonCopy(o['name'], file);
              zroot.file(o['name'], o['zdata']);
            }
          }
        }
      });
    });
    zip.generateAsync({type: 'blob'}).then(function (zipData) {
      FileSaver(zipData, zipname);
      this.gps.updateSpinner(false);
    }.bind(this));
  }

  createJsonCopy(name: string, jsonData: any): any {
    return {
      zdata: JSON.stringify(jsonData.body.risultati),
      name: name.split('.csv').join('.json')
    };
  }

  jsonToCsv(name: string, jsonData: any): string {
    let _csv: string = '';
    switch(name) {
      case 'Eventi.csv':
        let _jsonArray: any[] = jsonData.risultati;
        let _keys = [];
        _keys = this._elaborateKeys(_jsonArray);
        _jsonArray.forEach((_json, index) => {
          if(index == 0) {
            let _mappedKeys = _keys.map((key) => {
              return '"'+key+'"';
            });
            _csv = _mappedKeys.join(', ')+'\r\n';
          }
          let row: string[] = [];
          _keys.forEach((_key) => {
            let _val = '';
            if (_json[_key]) {
              if (typeof _json[_key] === 'object') {
                _val = JSON.stringify(_json[_key]);
              } else {
                _val = (_json[_key]).toString().replace(/("("")*)+/g, '"$1');
              }
            }
            row.push('"'+(_val || 'n/a')+'"');
          });
          _csv += row.join(', ')+'\r\n';
        });
        break;
    }

    return _csv;
  }

  /**
   * Elaborate structure
   * @param {string} name
   * @param {any} file
   * @returns {any}
   * @private
   */
  protected _elaborate(name: string, file: any): any {
    let zdata = file.body;
    if(name.indexOf('csv') != -1) {
      zdata = this.jsonToCsv(name, file.body);
    }
    return { zdata: zdata, name: name };
  }

  /**
   * Elaborate keys
   * @param {string} array
   * @returns {string[]}
   * @private
   */
  protected _elaborateKeys(array: any): string[] {
    let _keys = [];
    array.forEach((item) => {
      Object.keys(item).forEach((key) => {
        if(_keys.indexOf(key) == -1) {
          _keys.push(key);
        }
      });
    });
    return _keys;
  }
}
