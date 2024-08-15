# FINT Events Collector

FINT Events Collector er en tjeneste som samler inn og lagrer hendelser fra FINT-plattformen. Tjenesten er konfigurert som en CronJob i Kubernetes og kjører daglig for å hente hendelser fra en spesifisert URL og lagre dem i en Azure Blob Storage-container.
