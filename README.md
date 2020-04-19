# Yandex cast Bot
Телеграм бот для отправки любого youtube видео на Yandex станцию.

1. git clone
2. mvn clean package -DskipTests=true
3. create workdir, for example /opt/yacast
4. cp target/ya-cast-bot-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/yacast
5. cp -r target/config /opt/yacast
6. cp -r target/log /opt/yacast
7. Fill config (config/yacast.cfg) with values
8. run app with java -jar ya-cast-bot-1.0-SNAPSHOT-jar-with-dependencies.jar
or with bash script 
```bash
#!/bin/sh
#

/usr/bin/java -jar ya-cast-bot.jar >/dev/null 2>&1 &
pid=$!
echo ${pid}
echo ${pid} > yacast.pid
```  
