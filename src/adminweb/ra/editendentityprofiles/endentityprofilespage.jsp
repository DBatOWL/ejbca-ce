<% /* editusergroups.jsp
    *
    * Main user group editing page, included from ejbcaathorization.jsp 
    * 
    * Created on  14 mars 2002, 11:56
    *
    * author  Philip Vendil */ %>

<% 
  TreeMap profiles     = ejbcarabean.getAuthorizedEndEntityProfileNames(); 
%>


<div align="center">
  <p><H1><%= ejbcawebbean.getText("ENDENTITYPROFILES") %></H1></p>
 <!-- <div align="right"><A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ra_help.html") + "#profiles"%>")'>
    <u><%= ejbcawebbean.getText("HELP") %></u> </A> -->
  </div>
  <form name="editprofiles" method="post"  action="<%= THIS_FILENAME%>">
    <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_EDIT_PROFILES %>'>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
    <% if(triedtoeditemptyprofile){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("YOUCANTEDITEMPTYPROFILE") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(triedtodeleteemptyprofile){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("YOUCANTDELETEEMPTYPROFILE") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %> 
    <% if(profileexists){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("PROFILEALREADYEXISTS") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(profiledeletefailed){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("COULDNTDELETEPROFILE") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
    <% if(cannotcloneempty){ %> 
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H4 id="alert"><%= ejbcawebbean.getText("YOUCANNOTUSEEMPTYPROFILE") %></H4></td>
        <td width="35%"></td>
      </tr>
    <% } %>
      <tr> 
        <td width="5%"></td>
        <td width="60%"><H3><%= ejbcawebbean.getText("CURRENTENDENTITYPROFILES") %></H3></td>
        <td width="35%"></td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="60%">
          <select name="<%=SELECT_PROFILE%>" size="15"  >
            <% Iterator iter = profiles.keySet().iterator();
               while(iter.hasNext()){
                 String profilename = (String) iter.next();%>
              <option value="<%=profilename%>"> 
                  <%= profilename %>
               </option>
            <%}%>
              <option value="">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</option>
          </select>
          </td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="60%"> 
          <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
              <td>
                <input type="submit" name="<%= BUTTON_EDIT_PROFILE %>" value="<%= ejbcawebbean.getText("EDITPROFILE") %>">
              </td>
              <td>
             &nbsp; 
              </td>
              <td>
                <input class=buttonstyle type="submit" onClick="return confirm('<%= ejbcawebbean.getText("AREYOUSURE") %>');" name="<%= BUTTON_DELETE_PROFILE %>" value="<%= ejbcawebbean.getText("DELETEPROFILE") %>">
              </td>
            </tr>
          </table> 
        </td>
        <td width="35%"> </td>
      </tr>
    </table>
   
  <p align="left"> </p>
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
      <tr> 
        <td width="5%"></td>
        <td width="95%"><H3><%= ejbcawebbean.getText("ADDPROFILE") %></H3></td>
      </tr>
      <tr> 
        <td width="5%"></td>
        <td width="95%"> 
          <input type="text" name="<%=TEXTFIELD_PROFILENAME%>" size="40" maxlength="255">   
          <input type="submit" name="<%= BUTTON_ADD_PROFILE%>" onClick='return checkfieldforlegalchars("document.editprofiles.<%=TEXTFIELD_PROFILENAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("ADDPROFILE") %>">&nbsp;&nbsp;&nbsp;
          <input type="submit" name="<%= BUTTON_RENAME_PROFILE%>" onClick='return checkfieldforlegalchars("document.editprofiles.<%=TEXTFIELD_PROFILENAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("RENAMESELECTED") %>">&nbsp;&nbsp;&nbsp;
          <input type="submit" name="<%= BUTTON_CLONE_PROFILE%>" onClick='return checkfieldforlegalchars("document.editprofiles.<%=TEXTFIELD_PROFILENAME%>","<%= ejbcawebbean.getText("ONLYCHARACTERS") %>")' value="<%= ejbcawebbean.getText("USESELECTEDASTEMPLATE") %>">
        </td>
      </tr>
      <tr> 
        <td width="5%">&nbsp; </td>
        <td width="95%">&nbsp;</td>
      </tr>
    </table>
  </form>
  <p align="center">&nbsp;</p>
  <p>&nbsp;</p>
</div>

