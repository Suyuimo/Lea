# Lea
Modular Matrix Bot

## Projektstruktur
```
/opt/lea/
├── lea-core.jar      ← die ausführbare JAR
├── config/
│   └── lea.yml       ← Konfiguration
├── modules/          ← Plugin-JARs
└── log/              ← Logdateien
```

## Development

Config vorbereiten und Bot direkt starten:
```bash
cp config/lea.example.yml config/lea.yml
./gradlew :lea-core:run
```

## Production Deployment (Systemd)

### 1. Benutzer und Verzeichnis anlegen
```bash
adduser lea
mkdir -p /opt/lea/modules /opt/lea/config /opt/lea/log
chown -R lea:lea /opt/lea
```

### 2. JAR bauen und deployen
```bash
./gradlew :lea-core:jar
cp lea-core/build/libs/lea-core-*.jar /opt/lea/lea-core.jar
cp config/lea.example.yml /opt/lea/config/lea.yml
# lea.yml anpassen!
nano /opt/lea/config/lea.yml
```

### 3. Systemd-Service einrichten

`/etc/systemd/system/lea.service`:
```ini
[Unit]
Description=Lea Signal Bot
After=network.target

[Service]
Type=simple
User=lea
WorkingDirectory=/opt/lea
ExecStart=/usr/bin/java -jar /opt/lea/lea-core.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```
```bash
sudo systemctl daemon-reload
sudo systemctl enable lea
sudo systemctl start lea
```

### 4. Logs
```bash
journalctl -u lea -f
```

### 5. Plugins deployen
```bash
cp mein-plugin.jar /opt/lea/modules/
sudo systemctl restart lea
```