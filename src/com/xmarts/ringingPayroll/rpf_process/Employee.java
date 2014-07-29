	/************************************************************************************ 
 * Copyright (C) 2009 Openbravo S.L.U. 
 * Licensed under the Openbravo Commercial License version 1.0 
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html 
 ************************************************************************************/

package com.xmarts.ringingPayroll.rpf_process;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import java.lang.Exception; 

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.security.PrivateKey;
import java.security.Signature;
import org.apache.commons.ssl.PKCS8Key;
import java.io.BufferedInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.GeneralSecurityException; 

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.Connection;
import org.hibernate.HibernateException;
import org.hibernate.criterion.Expression;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.xmlEngine.XmlDocument;

import jxl.*; 
import java.io.*; 
import com.xmarts.ringingPayroll.RPFUploadEmp;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.ad.access.User;


public class Employee extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  //private FileGeneration fileGeneration = new FileGeneration();

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Posted: doPost");

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strKey = vars.getGlobalVariable("inprpfUploadEmpId", "Employee|RPF_Upload_Emp_ID");
      System.out.println("Clave Default:" + strKey);
      String strWindowId = vars.getGlobalVariable("inpwindowId", "Employee|windowId");
      String strTabId = vars.getGlobalVariable("inpTabId", "Employee|tabId");
      String strProcessId = vars.getGlobalVariable("inpProcessed", "Employee|processId", "");
      String strPath = vars.getGlobalVariable("inpPath", "Employee|path", strDireccion
          + request.getServletPath());
      String strPosted = null;
      String strEmployeed = "N";
      String strDocNo = null;
      String strWindowPath = Utility.getTabURL(this, strTabId, "E");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet; 

      if (strEmployeed.equals("Y"))
	advisePopUp(request, response, Utility.messageBD(this, "ERROR", vars.getLanguage()),
	    Utility.messageBD(this, "CFDI_Employee", vars.getLanguage()));

      printPage(response, vars, strKey, strWindowId, strTabId, strProcessId, strPath, strPosted,
          strWindowPath, strDocNo);

    } else if (vars.commandIn("SAVE")) {

      String strKey = vars.getGlobalVariable("inprpfUploadEmpId", "Employee|RPF_Upload_Emp_ID");
      String strWindowId = vars.getRequestGlobalVariable("inpwindowId", "Employee|windowId");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "Employee|tabId");
      String strProcessId = vars.getRequestGlobalVariable("inpProcessed", "Employee|processId");
      String strPath = vars.getRequestGlobalVariable("inpPath", "Employee|path");
      String strPosted = null;
      String strDocNo = null;
      String strWindowPath = Utility.getTabURL(this, strTabId, "E");
      String strFilePath = vars.getRequestGlobalVariable("inpFilePath", "Employee|FileName");

      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;

        OBError myMessage = process(response, vars, strKey, strWindowId, strTabId, strProcessId,
        strPath, strPosted, strWindowPath, strDocNo);
        vars.setMessage(strTabId, myMessage);
	printPageClosePopUp(response, vars, strWindowPath);
     
    } else {
      advisePopUp(request, response, Utility.messageBD(this, "ERROR", vars.getLanguage()), Utility
          .messageBD(this, "ERROR", vars.getLanguage()));
    }
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strKey,
      String windowId, String strTab, String strProcessId, String strPath, String strPosted,
      String strFilePath, String strDocNo) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: Button process Employee Electronic Invoice");
//System.out.println("metodo prong");
    String[] discard = { "" };
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("com/xmarts/ringingPayroll/rpf_process/Employee", discard).createXmlDocument();
    xmlDocument.setParameter("key", strKey);
    xmlDocument.setParameter("window", windowId);
    xmlDocument.setParameter("tab", strTab);
    xmlDocument.setParameter("processed", strProcessId);
    xmlDocument.setParameter("path", strPath);
    xmlDocument.setParameter("posted", strPosted);
    xmlDocument.setParameter("docNo", strDocNo);

    {
      OBError myMessage = vars.getMessage("Employee");
      vars.removeMessage("Employee");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("cInvoiceId", strKey);

    if (!strFilePath.equals("")) {
      xmlDocument.setParameter("inpFilePath", strFilePath);
      xmlDocument.setParameter("inpOnLoad", "onLoadClose();");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError process(HttpServletResponse response, VariablesSecureApp vars, String strKey,String windowId, String strTab, String strProcessId, String strPath, String strPosted,
      String strWindowPath, String strDocNo) {
     OBError myMessage = new OBError();
     myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
      System.out.println("entro al metodo" + strKey);
      String g=strKey;
      String r=null;
      for(int x=2;x<g.length();x=x+3){
           if(x==2){
           r=""+g.charAt(x-2)+g.charAt(x-1)+g.charAt(x);   
           }else{
           r=r+"/"+g.charAt(x-2)+g.charAt(x-1)+g.charAt(x);
           }
           System.out.println(x);
           if(x==29){
           r=r+"/"+g.charAt(x+1)+g.charAt(x+2)+"/";   
           }
       }
      System.out.println("Ruta del id divido "+r);
        
      String strDireccion = globalParameters.strFTPDirectory+"/"+"06D7B5E8C9D54AB28DA8CBD95D30F833"+"/"+r;
      
      System.out.println("Direccion del attachments "+strDireccion);

      try{

      String archivo = UploadData.file(new DalConnectionProvider(),strKey);
      if(archivo.equals("")){
       myMessage.setMessage("Es necesario adjuntar el archivo de Excel");
       myMessage.setType("Error");
       myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error", OBContext.getOBContext().getLanguage().getLanguage()));
        return myMessage;   
      }
      String archivoDestino = strDireccion+archivo;
      System.out.println("Nombre del archivo "+archivoDestino);

      RPFUploadEmp rpfcon = OBDal.getInstance().get(RPFUploadEmp.class, strKey);

      Client client = rpfcon.getClient();
      String clien = client.getId();
      System.out.println("Client "+clien);

      Organization organiz = rpfcon.getOrganization();
      String org = organiz.getId();
      System.out.println("Organization "+org);

      User user = rpfcon.getCreatedBy();
      String use = user.getId();
      System.out.println("User "+use);

      try { 
            Workbook archivoExcel = Workbook.getWorkbook(new File( 
            archivoDestino)); 
            System.out.println("Número de Hojas\t" + archivoExcel.getNumberOfSheets()); 
                for (int sheetNo = 0; sheetNo < archivoExcel.getNumberOfSheets(); sheetNo++) // Recorre                                                                                                                                                       
                { 
                    Sheet hoja = archivoExcel.getSheet(sheetNo); 
                    int numColumnas = hoja.getColumns();
                    System.out.println("Columnas "+numColumnas);
                    if(numColumnas >= 32){
                        //System.out.println("El excel tiene mas columnas de las requeridas, favor de validarlo");
                        myMessage.setMessage("El excel tiene mas columnas de las requeridas, favor de validarlo");
                        myMessage.setType("Error");
                        myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error", OBContext.getOBContext().getLanguage().getLanguage()));
                       return myMessage; 
                    }else{
                        if(numColumnas <= 30){
                         //System.out.println("El excel tiene menos columnas de las requeridas, favor de validarlo");
                         myMessage.setMessage("El excel tiene menos columnas de las requeridas, favor de validarlo");
                         myMessage.setType("Error");
                         myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error", OBContext.getOBContext().getLanguage().getLanguage()));
                       return myMessage;    
                        }else{
                            int numFilas = hoja.getRows(); 
                            System.out.println("Filas "+numFilas);
                            String data; 
                            System.out.println("Nombre de la Hoja\t"+ archivoExcel.getSheet(sheetNo).getName()); 
                            UploadData.elim(this);
                            System.out.println("Borrando lineas antes cargardas");
                            for (int fila = 0; fila < numFilas; fila++) { // Recorre cada 
                            // fila de la  hoja 
                                        String nom=null;
                                    String rfc=null;
                                    String calle=null;
                                    String noext=null;
                                    String noint=null;
                                    String col=null;
                                    String loc=null;
                                    String mun=null;
                                    String est=null;
                                    String pais=null;
                                    String cp=null;
                                    String regis=null;
                                    String num=null;
                                    String curp=null;
                                    String tiporeg=null;
                                    String numseg=null;
                                    String depa=null;
                                    String banco=null;
                                    String fechaini=null;
                                    String antigue=null;
                                    String puesto=null;
                                    String tipocon=null;
                                    String tipojor=null;
                                    String salariob=null;
                                    String riesgo=null;
                                    String salariod=null;
                                    String met=null;
                                    String cond=null;
                                    String mon=null;
                                    String forma=null;
                                    String numcuenta=null;
                                    for (int columna = 0; columna < numColumnas; columna++) { // Recorre                                                                                
                                        // cada columna de la fila 
                                        data = hoja.getCell(columna, fila).getContents();
                                        if(columna == 0){  
                                           nom = data;
                                        }
                                        if(columna == 1){
                                           rfc = data;
                                        }
                                        if(columna == 2){
                                           calle=data; 
                                        }
                                        if(columna == 3){
                                           noext = data; 
                                        }
                                        if(columna == 4){
                                           noint = data;
                                        }
                                        if(columna == 5){
                                           col = data;
                                        }
                                        if(columna == 6){
                                           loc = data;
                                        }
                                        if(columna == 7){
                                           mun = data;
                                        }
                                        if(columna== 8){
                                           est = data;
                                        }
                                        if(columna== 9){
                                           pais = data;                                      
                                        }
                                         if(columna== 10){
                                           cp = data;                                      
                                        }
                                        if(columna== 11){
                                           regis = data;                                      
                                        }
                                        if(columna== 12){
                                           num = data;                                      
                                        }
                                         if(columna== 13){
                                           curp = data;                                      
                                        }
                                        if(columna== 14){
                                           tiporeg = data;                                      
                                        }
                                        if(columna== 15){
                                           numseg = data;                                      
                                        }
                                        if(columna== 16){
                                           depa = data;                                      
                                        }
                                        if(columna== 17){
                                           banco = data;                                      
                                        }
                                        if(columna== 18){
                                           fechaini = data;                                      
                                        }
                                        if(columna== 19){
                                           antigue = data;                                      
                                        }
                                        if(columna== 20){
                                           puesto = data;                                      
                                        }
                                        if(columna== 21){
                                           tipocon = data;                                      
                                        }
                                        if(columna== 22){
                                           tipojor = data;                                      
                                        }
                                        if(columna== 23){
                                           salariob = data;                                      
                                        }
                                        if(columna== 24){
                                           riesgo = data;                                      
                                        }
                                        if(columna== 25){
                                           salariod = data;                                      
                                        }
                                        if(columna== 26){
                                           met = data;                                      
                                        }
                                        if(columna== 27){
                                           cond = data;                                      
                                        }
                                        if(columna== 28){
                                           mon = data;                                      
                                        }
                                        if(columna== 29){
                                           forma = data;                                      
                                        }
                                        if(columna== 30){
                                           numcuenta = data;                                      
                                        }   
                                        //System.out.println(columna+" "+fila);
                                        //System.out.print(data + " ")1; 
                                    } 
                                  if(nom.equals("") && rfc.equals("") && calle.equals("")){
                                    System.out.println("entro al break");
                                    break;
                                  }else{       
                                    //System.out.println(num+" "+con+" "+percep+" "+deducci+" "+dias+"\n"); 
                                    System.out.println("Guardando datos");
                                    if(nom.equals("NombreEmpleado") && rfc.equals("RFC") && calle.equals("Calle")){
                                      System.out.println("entro al encabezado :p");
                                    }else{
                                    UploadData.uplo(this,clien,org,use,use,nom,rfc,calle,noext,noint,col,loc,mun,est,pais,cp,num,curp,tiporeg,numseg,depa,banco,fechaini,antigue,puesto,tipocon,tipojor,salariob,riesgo,salariod,met,cond,mon,forma,numcuenta,regis);
                                    System.out.println("Termino de guardar la informacion");
                                    }
                                  }
                            }
                        }//cierra el else de las columnas
                    } 
                }
            } catch (Exception ioe) { 
              System.out.println("Entro al try exception");
              myMessage.setMessage(ioe.getMessage());
              myMessage.setType("Error");
              myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error", OBContext.getOBContext().getLanguage().getLanguage()));
          return myMessage;
            } 
      }catch (ServletException e) {
        System.out.println("Entro al try Servelet exception");
        myMessage.setMessage(e.getMessage());
        myMessage.setType("Error");
        myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Error", OBContext.getOBContext().getLanguage().getLanguage()));
          return myMessage; 
      }
   myMessage.setMessage("El excel fue cargado exitosamente.");
   myMessage.setType("Success");
   myMessage.setTitle(Utility.messageBD(new DalConnectionProvider(), "Success", OBContext.getOBContext().getLanguage().getLanguage()));

    return myMessage;
  }







}

