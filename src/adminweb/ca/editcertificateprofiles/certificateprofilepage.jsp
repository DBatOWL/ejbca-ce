<%               
  CertificateProfile certificateprofiledata = cabean.getCertificateProfile(certprofile.trim());

 
  String[] SUPERADMIN_TYPE_NAMES = {"ENDENTITY", "SUBCA", "ROOTCA"};
  int[] SUPERADMIN_TYPE_IDS = {SecConst.CERTTYPE_ENDENTITY,SecConst.CERTTYPE_SUBCA , SecConst.CERTTYPE_ROOTCA};
   
  String[] TYPE_NAMES = {"ENDENTITY"};
  int[] TYPE_IDS = {SecConst.CERTTYPE_ENDENTITY};
  if(issuperadministrator){
    TYPE_NAMES = SUPERADMIN_TYPE_NAMES;
    TYPE_IDS = SUPERADMIN_TYPE_IDS;
  }


  Collection authorizedcas = cabean.getAuthorizedCAs();
  HashMap caidtonamemap = cabean.getCAIdToNameMap();

  HashMap publisheridtonamemap = ejbcawebbean.getInformationMemory().getPublisherIdToNameMap();

  int row = 0;
%>
<SCRIPT language="JavaScript">
<!--  

function checkusefield(usefield, criticalfield){
  var usebox = eval("document.editcertificateprofile." + usefield);
  var cribox = eval("document.editcertificateprofile." + criticalfield);
  if(usebox.checked){
    cribox.disabled = false;
  }
  else{
    cribox.checked=false;
    cribox.disabled = true;
  }
}

function checkusecrldisturifield(){
  if(document.editcertificateprofile.<%=CHECKBOX_CRLDISTRIBUTIONPOINT %>.checked){
    document.editcertificateprofile.<%= CHECKBOX_CRLDISTRIBUTIONPOINTCRITICAL %>.disabled = false;
    document.editcertificateprofile.<%= TEXTFIELD_CRLDISTURI %>.disabled = false;
    document.editcertificateprofile.<%= TEXTFIELD_CRLDISTURI %>.value = "<%= globalconfiguration.getStandardCRLDistributionPointURI() %>";
  }
  else{
    document.editcertificateprofile.<%= CHECKBOX_CRLDISTRIBUTIONPOINTCRITICAL %>.disabled = true;
    document.editcertificateprofile.<%= CHECKBOX_CRLDISTRIBUTIONPOINTCRITICAL %>.checked = false;
    document.editcertificateprofile.<%= TEXTFIELD_CRLDISTURI %>.disabled = true;
    document.editcertificateprofile.<%= TEXTFIELD_CRLDISTURI %>.value = "";
  }

}

function checkuseocspservicelocatorfield(){
  if(document.editcertificateprofile.<%=CHECKBOX_USEOCSPSERVICELOCATOR %>.checked){    
    document.editcertificateprofile.<%= TEXTFIELD_OCSPSERVICELOCATOR %>.disabled = false;
    document.editcertificateprofile.<%= TEXTFIELD_OCSPSERVICELOCATOR %>.value = "<%= globalconfiguration.getStandardOCSPServiceLocatorURI() %>";
  }
  else{
    document.editcertificateprofile.<%= TEXTFIELD_OCSPSERVICELOCATOR %>.disabled = true;
    document.editcertificateprofile.<%= TEXTFIELD_OCSPSERVICELOCATOR %>.value = "";
  }
}


function typechanged(){
  var seltype = document.editcertificateprofile.<%=SELECT_TYPE %>.options.selectedIndex;
  var type = document.editcertificateprofile.<%=SELECT_TYPE %>.options[seltype].value; 

  if(type == <%= SecConst.CERTTYPE_ENDENTITY %>){    
    document.editcertificateprofile.<%=SELECT_AVAILABLEPUBLISHERS %>.disabled=false;
  }else{
    document.editcertificateprofile.<%=SELECT_AVAILABLEPUBLISHERS %>.disabled=true;
  } 
}

function checkusecertificatepoliciesfield(){
  if(document.editcertificateprofile.<%=CHECKBOX_USECERTIFICATEPOLICIES %>.checked){
    document.editcertificateprofile.<%= CHECKBOX_CERTIFICATEPOLICIESCRITICAL %>.disabled = false;
    document.editcertificateprofile.<%= TEXTFIELD_CERTIFICATEPOLICYID %>.disabled = false;
    document.editcertificateprofile.<%= TEXTFIELD_CERTIFICATEPOLICYID %>.value = "";
  }
  else{
    document.editcertificateprofile.<%= CHECKBOX_CERTIFICATEPOLICIESCRITICAL %>.disabled = true;
    document.editcertificateprofile.<%= CHECKBOX_CERTIFICATEPOLICIESCRITICAL %>.checked = false;
    document.editcertificateprofile.<%= TEXTFIELD_CERTIFICATEPOLICYID %>.disabled = true;
    document.editcertificateprofile.<%= TEXTFIELD_CERTIFICATEPOLICYID %>.value = "";
  }
}

function checkuseextendedkeyusagefield(){
  if(document.editcertificateprofile.<%=CHECKBOX_USEEXTENDEDKEYUSAGE %>.checked){
    document.editcertificateprofile.<%= SELECT_EXTENDEDKEYUSAGE %>.disabled = false;
    document.editcertificateprofile.<%= CHECKBOX_EXTENDEDKEYUSAGECRITICAL %>.disabled = false;
  }
  else{
    document.editcertificateprofile.<%= SELECT_EXTENDEDKEYUSAGE %>.disabled = true;
    document.editcertificateprofile.<%= CHECKBOX_EXTENDEDKEYUSAGECRITICAL %>.disabled = true;
    document.editcertificateprofile.<%= CHECKBOX_EXTENDEDKEYUSAGECRITICAL %>.checked = false;
  }
}


function checkallfields(){
    var illegalfields = 0;

    if(!checkfieldfordecimalnumbers("document.editcertificateprofile.<%=TEXTFIELD_VALIDITY%>","<%= ejbcawebbean.getText("ONLYDECNUMBERSINVALIDITY") %>"))
      illegalfields++;
    
    var availablebitlengths = document.editcertificateprofile.<%= SELECT_AVAILABLEBITLENGTHS%>.options;
    var selected = 0;
    for(var i=0; i < availablebitlengths.length; i++){
      if(availablebitlengths[i].selected==true)
        selected++; 
    }

    if(selected == 0){
      alert("<%=  ejbcawebbean.getText("ONEAVAILABLEBITLENGTH") %>");
      illegalfields++; 
    }
   
     return illegalfields == 0;  
   } 
-->

</SCRIPT>
<div align="center"> 
  <h2><%= ejbcawebbean.getText("EDITCERTIFICATEPROFILE") %><br>
  </h2>
  <h3><%= ejbcawebbean.getText("CERTIFICATEPROFILE")+ " : " + certprofile %> </h3>
</div>
<form name="editcertificateprofile" method="post" action="<%=THIS_FILENAME %>">
  <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_EDIT_CERTIFICATEPROFILE %>'>
  <input type="hidden" name='<%= HIDDEN_CERTIFICATEPROFILENAME %>' value='<%=certprofile %>'>
  <table width="100%" border="0" cellspacing="3" cellpadding="3">
    <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="left"> 
          <h3>&nbsp;</h3>
        </div>
      </td>
      <td width="50%" valign="top"> 
        <div align="right">
        <A href="<%=THIS_FILENAME %>"><u><%= ejbcawebbean.getText("BACKTOCERTIFICATEPROFILES") %></u></A>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
   <!--     <A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ca_help.html") + "#certificateprofiles"%>")'>
        <u><%= ejbcawebbean.getText("HELP") %></u> </A></div> -->
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("VALIDITY") %> (<%= ejbcawebbean.getText("DAYS") %>)<br>&nbsp;
      </td>
      <td width="50%"> 
        <input type="text" name="<%=TEXTFIELD_VALIDITY%>" size="5" maxlength="255" 
           value="<%= certificateprofiledata.getValidity()  %>"><br>
      </td>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("USEBASICCONSTRAINTS") %> <br>  <%= ejbcawebbean.getText("BASICCONSTRAINTSCRITICAL") %>

      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_BASICCONSTRAINTS %>"   onClick="checkusefield('<%=CHECKBOX_BASICCONSTRAINTS %>', '<%=CHECKBOX_BASICCONSTRAINTSCRITICAL %>')" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseBasicConstraints()) 
                 out.write("CHECKED");
           %>> <br> 
          <input type="checkbox" name="<%=CHECKBOX_BASICCONSTRAINTSCRITICAL %>" value="<%=CHECKBOX_VALUE %>" 
           <%
               if(!certificateprofiledata.getUseBasicConstraints())
                 out.write(" disabled ");  
               else
               if(certificateprofiledata.getBasicConstraintsCritical())
                 out.write("CHECKED");
           %>> 
      </td>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("USEKEYUSAGE") %> <br>  <%= ejbcawebbean.getText("KEYUSAGECRITICAL") %>

      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_KEYUSAGE %>" onClick="checkusefield('<%=CHECKBOX_KEYUSAGE %>', '<%=CHECKBOX_KEYUSAGECRITICAL %>')" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseKeyUsage())
                 out.write("CHECKED");
           %>> <br> 
          <input type="checkbox" name="<%=CHECKBOX_KEYUSAGECRITICAL %>" value="<%=CHECKBOX_VALUE %>" 
           <%
               if(!certificateprofiledata.getUseKeyUsage())
                 out.write(" disabled ");  
               else
               if(certificateprofiledata.getKeyUsageCritical())
                 out.write("CHECKED");
           %>> 
      </td>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("SUBJECTKEYID") %> <br>  <%= ejbcawebbean.getText("SUBJECTKEYIDCRITICAL") %>

      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_SUBJECTKEYIDENTIFIER %>" onClick="checkusefield('<%=CHECKBOX_SUBJECTKEYIDENTIFIER %>', '<%=CHECKBOX_SUBJECTKEYIDENTIFIERCRITICAL %>')" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseSubjectKeyIdentifier())
                 out.write("CHECKED");
           %>> <br> 
          <input type="checkbox" name="<%=CHECKBOX_SUBJECTKEYIDENTIFIERCRITICAL %>" value="<%=CHECKBOX_VALUE %>" 
           <%
             if(!certificateprofiledata.getUseSubjectKeyIdentifier())
                 out.write(" disabled "); 
              else
              if(certificateprofiledata.getSubjectKeyIdentifierCritical())
                 out.write("CHECKED");
           %>> 
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
         <%= ejbcawebbean.getText("AUTHORITYKEYID") %> <br> <%= ejbcawebbean.getText("AUTHORITYKEYIDCRITICAL") %> 
      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_AUTHORITYKEYIDENTIFIER %>" onClick="checkusefield('<%=CHECKBOX_AUTHORITYKEYIDENTIFIER %>', '<%=CHECKBOX_AUTHORITYKEYIDENTIFIERCRITICAL %>')" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseAuthorityKeyIdentifier())
                 out.write("CHECKED");
           %>> <br> 
          <input type="checkbox" name="<%=CHECKBOX_AUTHORITYKEYIDENTIFIERCRITICAL %>" value="<%=CHECKBOX_VALUE %>" 
           <%
             if(!certificateprofiledata.getUseAuthorityKeyIdentifier())
                 out.write(" disabled ");  
             else
             if(certificateprofiledata.getAuthorityKeyIdentifierCritical())
                 out.write("CHECKED");
           %>> 
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("SUBJECTALTNAME") %> <br>  <%= ejbcawebbean.getText("SUBJECTALTNAMECRITICAL") %>

      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_SUBJECTALTERNATIVENAME %>" onClick="checkusefield('<%=CHECKBOX_SUBJECTALTERNATIVENAME %>', '<%=CHECKBOX_SUBJECTALTERNATIVENAMECRITICAL %>')" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseSubjectAlternativeName())
                 out.write("CHECKED");
           %>> <br> 
          <input type="checkbox" name="<%=CHECKBOX_SUBJECTALTERNATIVENAMECRITICAL %>" value="<%=CHECKBOX_VALUE %>" 
           <% 
             if(!certificateprofiledata.getUseSubjectAlternativeName())
                 out.write(" disabled "); 
              else
              if(certificateprofiledata.getSubjectAlternativeNameCritical())
                 out.write("CHECKED");
           %>> 
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("CRLDISTPOINT") %> <br>  <%= ejbcawebbean.getText("CRLDISTPOINTCRITICAL") %> <br> <%= ejbcawebbean.getText("CRLDISTPOINTURI") %>

      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_CRLDISTRIBUTIONPOINT %>" onClick="checkusecrldisturifield()" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseCRLDistributionPoint())
                 out.write("CHECKED");
           %>> <br> 
          <input type="checkbox" name="<%=CHECKBOX_CRLDISTRIBUTIONPOINTCRITICAL %>" value="<%=CHECKBOX_VALUE %>" 
           <%
               if(!certificateprofiledata.getUseCRLDistributionPoint())
                 out.write(" disabled "); 
               else
                 if(certificateprofiledata.getCRLDistributionPointCritical())
                 out.write("CHECKED");
           %>> <br> 
           <input type="text" name="<%=TEXTFIELD_CRLDISTURI%>" size="60" maxlength="255" 
           <%       if(!certificateprofiledata.getUseCRLDistributionPoint())
                      out.write(" disabled "); 
                    else 
                      if(!certificateprofiledata.getCRLDistributionPointURI().equals(""))
                       out.write(" value=\"" + certificateprofiledata.getCRLDistributionPointURI() + "\""); 
                      else
                       out.write(" value=\"" + globalconfiguration.getStandardCRLDistributionPointURI()+ "\"");%>>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("OCSPSERVICELOCATOR") %> <br> <%= ejbcawebbean.getText("OCSPSERVICELOCATORURI") %>
      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_USEOCSPSERVICELOCATOR %>" onClick="checkuseocspservicelocatorfield()" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseOCSPServiceLocator())
                 out.write("CHECKED");
           %>> <br> 
           <input type="text" name="<%=TEXTFIELD_OCSPSERVICELOCATOR%>" size="60" maxlength="255" 
           <%       if(!certificateprofiledata.getUseOCSPServiceLocator())
                      out.write(" disabled "); 
                    else 
                      if(!certificateprofiledata.getOCSPServiceLocatorURI().equals(""))
                       out.write(" value=\"" + certificateprofiledata.getOCSPServiceLocatorURI() + "\""); 
                      else
                       out.write(" value=\"" + globalconfiguration.getStandardOCSPServiceLocatorURI()+ "\"");%>>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("CERTIFICATEPOLICIES") %> <br>  <%= ejbcawebbean.getText("CERTIFICATEPOLICIESCRIT") %> <br> <%= ejbcawebbean.getText("CERTIFICATEPOLICYID") %>

      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_USECERTIFICATEPOLICIES %>" onClick="checkusecertificatepoliciesfield()" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseCertificatePolicies())
                 out.write("CHECKED");
           %>> <br> 
          <input type="checkbox" name="<%=CHECKBOX_CERTIFICATEPOLICIESCRITICAL %>" value="<%=CHECKBOX_VALUE %>" 
           <%
               if(!certificateprofiledata.getUseCertificatePolicies())
                 out.write(" disabled "); 
               else
                 if(certificateprofiledata.getCertificatePoliciesCritical())
                 out.write("CHECKED");
           %>> <br> 
           <input type="text" name="<%=TEXTFIELD_CERTIFICATEPOLICYID%>" size="60" maxlength="255" 
           <%       if(!certificateprofiledata.getUseCertificatePolicies())
                      out.write(" disabled "); 
                    else 
                      out.write(" value=\"" + certificateprofiledata.getCertificatePolicyId() + "\""); %>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%" valign="top" align="right">&nbsp;</td>
      <td width="50%" valign="top" align="right">&nbsp;</td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%" align="right"> 
        <%= ejbcawebbean.getText("KEYUSAGE") %> <br>&nbsp;
      </td>
      <td width="50%"> 
        <select name="<%=SELECT_KEYUSAGE%>" size="9" multiple >
           <%  boolean[] ku = certificateprofiledata.getKeyUsage();
                for(int i=0; i<keyusagetexts.length;i++){ %>
           <option  value="<%= i %>" 
              <% if(ku[i]) out.write(" selected "); %>> 
              <%= ejbcawebbean.getText(keyusagetexts[i]) %>
           </option>
           <%   } %> 
        </select>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("ALLOWKEYUSAGEOVERRIDE") %>
      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_ALLOWKEYUSAGEOVERRIDE %>"  value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getAllowKeyUsageOverride())
                 out.write("CHECKED");
           %>> 
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%"  align="right"> 
        <%= ejbcawebbean.getText("USEEXTENDEDKEYUSAGE") %><br><%= ejbcawebbean.getText("EXTENDEDKEYUSAGECRITICAL") %>
      </td>
      <td width="50%">
           <input type="checkbox" name="<%=CHECKBOX_USEEXTENDEDKEYUSAGE %>"  onclick="checkuseextendedkeyusagefield()" value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getUseExtendedKeyUsage())
                 out.write("CHECKED");
           %>><br> 
           <input type="checkbox" name="<%=CHECKBOX_EXTENDEDKEYUSAGECRITICAL %>"  <% if(!certificateprofiledata.getUseExtendedKeyUsage()) out.write(" disabled "); %> value="<%=CHECKBOX_VALUE %>" 
           <% if(certificateprofiledata.getExtendedKeyUsageCritical())
                 out.write("CHECKED");
           %>>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%" align="right"> 
        <%= ejbcawebbean.getText("EXTENDEDKEYUSAGE") %> <br>&nbsp;
      </td>
      <td width="50%"> 
        <select name="<%=SELECT_EXTENDEDKEYUSAGE%>" size="<%=extendedkeyusagetexts.length%>" multiple <% if(!certificateprofiledata.getUseExtendedKeyUsage()) out.write(" disabled "); %>>
           <%  ArrayList eku = certificateprofiledata.getExtendedKeyUsage();
                for(int i=0; i<extendedkeyusagetexts.length;i++){ %>
           <option  value="<%= i %>" 
              <% for(int j=0; j < eku.size(); j++) 
                   if(((Integer) eku.get(j)).intValue() == i ) out.write(" selected "); %>> 
              <%= ejbcawebbean.getText(extendedkeyusagetexts[i]) %>
           </option>
           <%   } %> 
        </select>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%" align="right"> 
        <%= ejbcawebbean.getText("AVAILABLEBITLENGTHS") %> <br>&nbsp;
      </td>
      <td width="50%"> 
        <select name="<%=SELECT_AVAILABLEBITLENGTHS%>" size="5" multiple >
           <%  int[] availablebits = certificateprofiledata.getAvailableBitLengths();
                for(int i=0; i<defaultavailablebitlengths.length;i++){ %>
           <option  value="<%= defaultavailablebitlengths[i] %>" 
              <% for(int j=0; j<availablebits.length;j++){
                   if(availablebits[j] == defaultavailablebitlengths[i])
                      out.write(" selected ");
                  }%>>
              <%= defaultavailablebitlengths[i] + " " + ejbcawebbean.getText("BITS") %>         
           </option>  
              <% } %>
        </select>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%" align="right"> 
        <%= ejbcawebbean.getText("AVAILABLECAS") %> <br>&nbsp;
      </td>
      <td width="50%"> 
        <select name="<%=SELECT_AVAILABLECAS%>" size="7" multiple >
           <% Collection usedcas = certificateprofiledata.getAvailableCAs(); 
              if(issuperadministrator){ %>
           <option  value="<%= CertificateProfile.ANYCA %>" 
              <% if(usedcas.contains(new Integer(CertificateProfile.ANYCA)))
                   out.write(" selected ");%>>
              <%= ejbcawebbean.getText("ANYCA") %>         
           </option>      
           <%   }
                Iterator iter = authorizedcas.iterator(); 
                while(iter.hasNext()){
                  Integer next = (Integer) iter.next(); %>
           <option  value="<%= next.intValue() %>" 
              <%    if(usedcas.contains(next))
                      out.write(" selected ");
                  %>>
              <%= caidtonamemap.get(next) %>         
           </option>  
              <% } %>
        </select>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%" align="right"> 
        <%= ejbcawebbean.getText("PUBLISHERS") %> <br>&nbsp;
      </td>
      <td width="50%"> 
        <select name="<%=SELECT_AVAILABLEPUBLISHERS%>" size="5" multiple  <% if(certificateprofiledata.getType() != SecConst.CERTTYPE_ENDENTITY) out.write(" disabled "); %>
           <%   Collection usedpublishers = certificateprofiledata.getPublisherList(); 
                iter = publisheridtonamemap.keySet().iterator(); 
                while(iter.hasNext()){
                  Integer next = (Integer) iter.next(); %>
           <option  value="<%= next.intValue() %>" 
              <%    if(usedpublishers.contains(next))
                      out.write(" selected ");
                  %>>
              <%= publisheridtonamemap.get(next) %>         
           </option>  
              <% } %>
        </select>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="50%" align="right"> 
        <%= ejbcawebbean.getText("TYPE") %> <br>&nbsp;
      </td>
      <td width="50%"> 
        <select name="<%=SELECT_TYPE%>" size="1" onchange='typechanged()' >
           <%  int type = certificateprofiledata.getType();
                for(int i=0; i<TYPE_IDS.length;i++){ %>
           <option  value="<%= TYPE_IDS[i] %>" 
              <%  if(TYPE_IDS[i] == type)
                    out.write(" selected ");
                  %>>
              <%= ejbcawebbean.getText(TYPE_NAMES[i]) %>         
           </option>  
              <% } %>
        </select>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="49%" valign="top">&nbsp;</td>
      <td width="51%" valign="top"> 
        <input type="submit" name="<%= BUTTON_SAVE %>" onClick='return checkallfields()' value="<%= ejbcawebbean.getText("SAVE") %>">
        <input type="submit" name="<%= BUTTON_CANCEL %>" value="<%= ejbcawebbean.getText("CANCEL") %>">
      </td>
    </tr>
  </table>
 </form>