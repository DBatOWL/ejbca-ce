<% HardTokenIssuerData issuerdata = tokenbean.getHardTokenIssuerData(alias);
   
   TreeMap hardtokenprofiles = ejbcawebbean.getInformationMemory().getHardTokenProfiles();

   boolean used = false;

   int row=0;
%>
<SCRIPT language="JavaScript">

  <!-- // Method to check all textfields for valid input -->
<!--
function checkallfields(){
    var illegalfields = 0;

    if(document.editissuer.<%=SELECT_AVAILABLEHARDTOKENPROFILES%>.options.selectedIndex == -1){
      alert("<%=  ejbcawebbean.getText("ATLEASTONETOKENPROFILE") %>");
      illegalfields++;
    }

    return illegalfields;
}
-->
</SCRIPT>
<div align="center"> 
  <h2><%= ejbcawebbean.getText("EDITHARDTOKENISSUER") %><br></h2>
</div>
<form name="editissuer" method="post" action="<%=THIS_FILENAME %>">
  <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_EDIT_ISSUER %>'>
  <input type="hidden" name='<%= HIDDEN_ALIAS %>' value='<%=alias %>'>
  <table width="100%" border="0" cellspacing="3" cellpadding="3">
    <tr id="Row<%=row++%2%>"> 
      <td width="15%" valign="top">
         &nbsp;
      </td>
      <td width="35%" valign="top"> 
        <div align="left"> 
          <h3>&nbsp;</h3>
        </div>
      </td>
      <td width="50%" valign="top"> 
        <div align="right">
        <A href="<%=THIS_FILENAME %>"><u><%= ejbcawebbean.getText("BACKTOHARDTOKENISSUERS") %></u></A>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     <!--   <A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("hardtoken_help.html") + "#edithardtokenissuers"%>")'>
        <u><%= ejbcawebbean.getText("HELP") %></u> </A></div> -->
      </td>
    </tr>
    <tr id="Row<%=row++%2%>"> 
      <td width="5%" valign="top">
         &nbsp;
      </td>
      <td width="25%"  align="right"> 
        <%= ejbcawebbean.getText("ALIAS") %> 
      </td>
      <td width="70%"> 
         <%=  issuerdata.getAlias() %>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="5%" valign="top">
        &nbsp;
      </td>
      <td width="25%"  align="right"> 
        <%= ejbcawebbean.getText("ADMINGROUP") %> 
      </td>
      <td width="70%"> 
        <%= adminidtonamemap.get(new Integer(issuerdata.getAdminGroupId())) %> 
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="5%" valign="top">
         &nbsp;
      </td>
      <td width="25%"  align="right"> 
          &nbsp;
      </td>
      <td width="70%"> 
         &nbsp;&nbsp; 
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="5%" valign="top">
         &nbsp;
      </td>
      <td width="25%"  align="right"> 
         <%= ejbcawebbean.getText("DESCRIPTION") %> 
      </td>
      <td width="70%"> 
          <textarea name="<%=TEXTFIELD_DESCRIPTION%>" cols=40 rows=6><% out.write(issuerdata.getHardTokenIssuer().getDescription());%></textarea>
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="5%" valign="top">
         &nbsp;
      </td>
      <td width="25%"  align="right"> 
          &nbsp;
      </td>
      <td width="70%"> 
         &nbsp;&nbsp; 
      </td>
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="5%" valign="top">
        &nbsp;
      </td>
      <td width="25%"  align="right"> 
        <%= ejbcawebbean.getText("AVAILABLEHARDTOKENPROFILES") %> 
      </td>
      <td width="70%"> 
        <select name="<%=SELECT_AVAILABLEHARDTOKENPROFILES %>" size="10" multiple >
            <% Iterator profiles = hardtokenprofiles.keySet().iterator();
               while(profiles.hasNext()){ 
                 String profilename = (String) profiles.next();
                 Integer profileid = (Integer) hardtokenprofiles.get(profilename);%>
           <option  value='<%= profileid.intValue()%>'
           <% ArrayList currenttokens = issuerdata.getHardTokenIssuer().getAvailableHardTokenProfiles();
              if(currenttokens != null){   
                Iterator iter = currenttokens.iterator();
                while(iter.hasNext())
                  if(((Integer) iter.next()).equals(profileid))
                    out.write(" selected "); 
              }%>><%= profilename %>
           </option>
            <% } %>
        </select>  
      </td> 
    </tr>
    <tr  id="Row<%=row++%2%>"> 
      <td width="5%" valign="top">
         &nbsp;
      </td>
      <td width="25%" align="right"> </td>
      <td width="70%" valign="top"> 
        <input type="submit" name="<%= BUTTON_SAVE %>" onClick='return checkallfields()' value="<%= ejbcawebbean.getText("SAVE") %>" >
        <input type="submit" name="<%= BUTTON_CANCEL %>" value="<%= ejbcawebbean.getText("CANCEL") %>">
      </td>
    </tr>
  </table>
 </form>