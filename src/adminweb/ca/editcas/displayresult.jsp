<% 
  String[] headlines = {"CERTREQGEN","CERTIFICATEGENERATED"};

  row = 0;
  String resultstring = null;
  if(filemode == CERTGENMODE){
    resultstring = cabean.getProcessedCertificateAsString();
  }else{
    resultstring = cabean.getPKCS10RequestDataAsString();
  }

  String pemlink =  globalconfiguration.getCaPath() + "/editcas/cacertreq?cmd=certreq";
  String pkcs7link = "";
  if(filemode == CERTGENMODE){
    pemlink =  globalconfiguration.getCaPath() + "/editcas/cacertreq?cmd=cert";
    pkcs7link = globalconfiguration.getCaPath() + "/editcas/cacertreq?cmd=certpkcs7";
  }
%>
<body > 
<div align="center">   
   <h2><%= ejbcawebbean.getText(headlines[filemode]) %><br></h2>
   <h3><%= ejbcawebbean.getText("CANAME")+ " : " + caname %> </h3>
</div>
  <table width="100%" border="0" cellspacing="3" cellpadding="3">
    <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="left"> 
          <h3>&nbsp;</h3>
        </div>
      </td>
      <td width="50%" valign="top"> 
        <div align="right">
        <A href="<%=THIS_FILENAME %>"><u><%= ejbcawebbean.getText("BACKTOCAS") %></u></A>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
   <!--     <A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ca_help.html") + "#cas"%>")'>
        <u><%= ejbcawebbean.getText("HELP") %></u> </A></div> -->
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="20%" valign="top" align="right"></td>
      <td width="80%" valign="top">     
        <form>
           <TEXTAREA rows='13' cols='100'><%=resultstring%></TEXTAREA>   
        </form>        
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="20%" valign="top" align="right"></td>
      <td width="80%" valign="top">     
        <A href="<%=pemlink%>"><u><%= ejbcawebbean.getText("DOWNLOADPEM") %></u></A><br>
      <% if(filemode == CERTGENMODE){ %>
        <A href="<%=pkcs7link%>"><u><%= ejbcawebbean.getText("DOWNLOADPEMASPKCS7") %></u></A><br><br>
      <% } %>        
        <A href="<%=THIS_FILENAME %>"><u><%= ejbcawebbean.getText("BACKTOCAS") %></u></A>
      </td>
    </tr>
  </table>