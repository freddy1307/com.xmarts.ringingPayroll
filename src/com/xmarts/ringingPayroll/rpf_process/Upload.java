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
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.xmlEngine.XmlDocument;

import jxl.*; 
import java.io.*; 
import com.xmarts.ringingPayroll.RPFConcept;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.ad.access.User;


public class Upload extends HttpSecureAppServlet {
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
      String strKey = vars.getGlobalVariable("inprpfConceptId", "Upload|RPF_Concept_ID");
      System.out.println("Clave Default:" + strKey);
      String strWindowId = vars.getGlobalVariable("inpwindowId", "Upload|windowId");
      String strTabId = vars.getGlobalVariable("inpTabId", "Upload|tabId");
      String strProcessId = vars.getGlobalVariable("inpProcessed", "Upload|processId", "");
      String strPath = vars.getGlobalVariable("inpPath", "Upload|path", strDireccion
          + request.getServletPath());
      String strPosted = null;
      String strUploadd = "N";
      String strDocNo = vars.getRequestGlobalVariable("inpdocumentno", "Upload|docNo");
      String strWindowPath = Utility.getTabURL(this, strTabId, "E");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet; 

      if (strUploadd.equals("Y"))
	advisePopUp(request, response, Utility.messageBD(this, "ERROR", vars.getLanguage()),
	    Utility.messageBD(this, "CFDI_Upload", vars.getLanguage()));

      printPage(response, vars, strKey, strWindowId, strTabId, strProcessId, strPath, strPosted,
          strWindowPath, strDocNo);

    } else if (vars.commandIn("SAVE")) {

      String strKey = vars.getGlobalVariable("inprpfConceptId", "Upload|RPF_Concept_ID");
      String strWindowId = vars.getRequestGlobalVariable("inpwindowId", "Upload|windowId");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "Upload|tabId");
      String strProcessId = vars.getRequestGlobalVariable("inpProcessed", "Upload|processId");
      String strPath = vars.getRequestGlobalVariable("inpPath", "Upload|path");
      String strPosted = null;
      String strDocNo = vars.getRequestGlobalVariable("inpdocumentno", "Upload|docNo");
      String strWindowPath = Utility.getTabURL(this, strTabId, "E");
      String strFilePath = vars.getRequestGlobalVariable("inpFilePath", "Upload|FileName");

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
      log4j.debug("Output: Button process Upload Electronic Invoice");
System.out.println("metodo prong");
    String[] discard = { "" };
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("com/xmarts/ringingPayroll/rpf_process/Upload", discard).createXmlDocument();
    xmlDocument.setParameter("key", strKey);
    xmlDocument.setParameter("window", windowId);
    xmlDocument.setParameter("tab", strTab);
    xmlDocument.setParameter("processed", strProcessId);
    xmlDocument.setParameter("path", strPath);
    xmlDocument.setParameter("posted", strPosted);
    xmlDocument.setParameter("docNo", strDocNo);

    {
      OBError myMessage = vars.getMessage("Upload");
      vars.removeMessage("Upload");
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
        
      String strDireccion = globalParameters.strFTPDirectory+"/"+"06D8871F1C124F4B93722C0742B2604A"+"/"+r;
      
      System.out.println("Direccion del attachments "+strDireccion);

      try{

      String archivo = UploadData.file(new DalConnectionProvider(),strKey);
      String archivoDestino = strDireccion+archivo;
      System.out.println("Nombre del archivo "+archivoDestino);

      RPFConcept rpfcon = OBDal.getInstance().get(RPFConcept.class, strKey);

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
                    if(numColumnas >= 6){
                        System.out.println("El excel tiene mas columnas de las requeridas, favor de validarlo");
                    }else{
                        if(numColumnas <= 4){
                         System.out.println("El excel tiene menos columnas de las requeridas, favor de validarlo");   
                        }else{
                            int numFilas = hoja.getRows(); 
                            System.out.println("Filas "+numFilas);
                            String data; 
                            System.out.println("Nombre de la Hoja\t"+ archivoExcel.getSheet(sheetNo).getName()); 

                            for (int fila = 0; fila < numFilas; fila++) { // Recorre cada 
                            // fila de la  hoja 
                                    String num=null;
                                    String con=null;
                                    String percep=null;
                                    String deducci=null;
                                    String dias=null;
                                    for (int columna = 0; columna < numColumnas; columna++) { // Recorre                                                                                
                                        // cada columna de la fila 
                                        data = hoja.getCell(columna, fila).getContents();
                                        if(columna == 1){
                                            num = data;
                                        }else{
                                            if(columna == 2){
                                                con = data;
                                            }else{
                                                if(columna == 3){
                                                   percep=data; 
                                                }else{
                                                    if(columna == 4){
                                                       deducci = data; 
                                                    }else{
                                                        if(columna == 5){
                                                            dias = data;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        //System.out.println(columna+" "+fila);
                                        //System.out.print(data + " "); 

                                    } 
                                  if(num.equals("") && con.equals("")){
                                    System.out.println("entro al break");
                                    break;
                                  }else{       
                                    System.out.println(num+" "+con+" "+percep+" "+deducci+" "+dias+"\n"); 
                                    System.out.println("Guardando datos");
                                    UploadData.cargar(this,clien,org,use,use,num,con,percep,deducci,strKey,dias);
                                    System.out.println("Termino de guardar la informacion");
                                  }
                            }
                        }
                    } 
                }
            } catch (Exception ioe) { 
            ioe.printStackTrace(); 
            } 
      }catch (ServletException e) {
        e.printStackTrace();
        myMessage.setMessage(Utility.messageBD(this, "FileCannotCreate", vars.getLanguage()));
        return myMessage;
      }

    return myMessage;
  }







}

