# WhatsUp
### A Java WhatsApp Client

WhatsUp is - as noted before - a WhatsApp Client written in Java. It's purpose is to let the user write Messages to other people using the WhatsApp Service using a PC instead of a mobile.

The mobilephone-number has to be in the format "ccxxxxx...", so for example: "49178276238746" (if this number really exists, call it). If you're using WhatsApp on your iPhone, the "imei" will be your MAC-Address ("AA:BB:CC:DD:EE:FF"). The username should be... your username. The password is an array of length 20 bytes, which is generated when you register your number using the code sent by SMS, for a simplification has been given the opportunity to be introduced as a Base64 encoded string. Is generated each time you register you number, but it is indefinite duration (for now ...). For obtain this password you can use a proxy: https://github.com/shirioko/WhatSNiff (Only for Rooted Android phones).

## Simple usagre

```java
WhatsAPI api = new WhatsAPI(PHONE_NUMBER, IMEI, USERNAME);

api.setHandler(new WhatsAppHandler()); //For incoming events like new message, login...
api.connect();
api.login(PW); //Your password in Base64 encoded
api.listen(); //Listen for incoming data
```

## I've got no idea for a appropriate title for this, so just deal with this really long thing...
So, actually right now this is a port of https://github.com/venomous0x/WhatsAPI.

People who helped me:
* Oliver Scheuch
* @sgade

### License
The License is GPL.
