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
import com.xmarts.ringingPayroll.RPFConcept;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.ad.access.User;


public class Ring extends HttpSecureAppServlet {
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
      String strKey = vars.getGlobalVariable("inprpfPayrollId", "Ring|RPF_Payroll_ID");
      System.out.println("Clave Default:" + strKey);
      String strWindowId = vars.getGlobalVariable("inpwindowId", "Ring|windowId");
      String strTabId = vars.getGlobalVariable("inpTabId", "Ring|tabId");
      String strProcessId = vars.getGlobalVariable("inpProcessed", "Ring|processId", "");
      String strPath = vars.getGlobalVariable("inpPath", "Ring|path", strDireccion
          + request.getServletPath());
      String strPosted = null;
      String strRingd = "N";
      String strDocNo = vars.getRequestGlobalVariable("inpdocumentno", "Ring|docNo");
      String strWindowPath = Utility.getTabURL(this, strTabId, "E");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet; 

      if (strRingd.equals("Y"))
	advisePopUp(request, response, Utility.messageBD(this, "ERROR", vars.getLanguage()),
	    Utility.messageBD(this, "CFDI_Ring", vars.getLanguage()));

      printPage(response, vars, strKey, strWindowId, strTabId, strProcessId, strPath, strPosted,
          strWindowPath, strDocNo);

    } else if (vars.commandIn("SAVE")) {

      String strKey = vars.getGlobalVariable("inprpfPayrollId", "Ring|RPF_Payroll_ID");
      String strWindowId = vars.getRequestGlobalVariable("inpwindowId", "Ring|windowId");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "Ring|tabId");
      String strProcessId = vars.getRequestGlobalVariable("inpProcessed", "Ring|processId");
      String strPath = vars.getRequestGlobalVariable("inpPath", "Ring|path");
      String strPosted = null;
      String strDocNo = vars.getRequestGlobalVariable("inpdocumentno", "Ring|docNo");
      String strWindowPath = Utility.getTabURL(this, strTabId, "E");
      String strFilePath = vars.getRequestGlobalVariable("inpFilePath", "Ring|FileName");

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
      log4j.debug("Output: Button process Ring Electronic Invoice");
    //System.out.println("metodo prong");
    String[] discard = { "" };
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("com/xmarts/ringingPayroll/rpf_process/Ring", discard).createXmlDocument();
    xmlDocument.setParameter("key", strKey);
    xmlDocument.setParameter("window", windowId);
    xmlDocument.setParameter("tab", strTab);
    xmlDocument.setParameter("processed", strProcessId);
    xmlDocument.setParameter("path", strPath);
    xmlDocument.setParameter("posted", strPosted);
    xmlDocument.setParameter("docNo", strDocNo);

    {
      OBError myMessage = vars.getMessage("Ring");
      vars.removeMessage("Ring");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("rpfPayrollId", strKey);

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
      System.out.println("entro al metodo :p" + strKey);


      /****FECHA DEL TIMBRADO ************/
      java.util.Date strfecha = new Date();
      String fecha = format1.format(strfecha);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      String fechatim = sdf.format(strfecha);





      StringBuffer StrXML = new StringBuffer(); 
      PrintStream xml = new PrintStream("Nomina"+strFolio+".xml", "UTF-8");


    return myMessage;
  }







}

