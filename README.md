# KitCaddy
KitCaddy is an open source Web Server written in Golang, with a Kit (KvalitetsIT) extension for hanlig OIO IDWS REST security protocol. 
See more <a href="https://www.digitaliser.dk/resource/3457606">here</a> about OIO IDWS REST.
KitCaddy works as a reverse proxy in front of the webservice. That's decouple the security protocol from the webservice.

![](documentation/KitCaddy-overview.png)

The images show the three configurations possible for KitCaddy; SAML, WSC and WSP. 
Configuration of the KitCaddy is done by configuration file ore with the Helm Chart. 

The Helm Chart is using KitCaddy as a sidecar to the selected webservice. As shown on the image above.
See Helm Chart documentation <a href="https://github.com/KvalitetsIT/kitcaddy/tree/master/helm/kitcaddy">here</a>

If configured by configuration file, KitCaddy expecting a configuration file placed here /config/configmap.yaml

The flow whit WSC and WSP this shown in the flowchart below.
![](documentation/WSC-WSP-flow.png)