<%               
  CustomPublisherContainer custompublisher = (CustomPublisherContainer) publisherhelper.publisherdata;
%>
   <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
         &nbsp;
        </div>
      </td>
      <td width="50%" valign="top"> 
         &nbsp;
      </td>
   </tr>
   <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
          <%= ejbcawebbean.getText("CUSTOMPUBLISHERSETTINGS") %>:
        </div>
      </td>
      <td width="50%" valign="top"> 
         &nbsp;
      </td>
    </tr>
    <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
          <%= ejbcawebbean.getText("CLASSPATH") %>
        </div>
      </td>
      <td width="50%" valign="top">   
        <input type="text" name="<%=EditPublisherJSPHelper.TEXTFIELD_CUSTOMCLASSPATH%>" size="30" maxlength="255" 
               value='<%= custompublisher.getClassPath()%>'>       
      </td>
    </tr>
    <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
          <%= ejbcawebbean.getText("PROPERTIESOFCUSTOM") %>
        </div>
      </td>
      <td width="50%" valign="top">          
         <textarea name="<%=EditPublisherJSPHelper.TEXTAREA_CUSTOMPROPERTIES%>" cols=40 rows=6><% out.write(custompublisher.getPropertyData());%></textarea>
      </td>
    </tr>
