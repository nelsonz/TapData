#include <Wire.h>
#include <Adafruit_NFCShield_I2C.h>

#define IRQ   (2)
#define RESET (3)  // Not connected by default on the NFC Shield

Adafruit_NFCShield_I2C nfc(IRQ, RESET);

void setup(void) {
  Serial.begin(9600);
  nfc.begin();
  
  // configure board to read RFID tags
  nfc.SAMConfig();
}

void loop(void) {
  uint8_t success;
  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer to store the returned UID
  uint8_t uidLength;                        // Length of the UID (4 or 7 bytes depending on ISO14443A card type)
    
  // Wait for an ISO14443A type cards (Mifare, etc.).  When one is found
  // 'uid' will be populated with the UID, and uidLength will indicate
  // if the uid is 4 bytes (Mifare Classic) or 7 bytes (Mifare Ultralight)
  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength);
  
  if (success && uidLength == 7) {
    for (int i=0; i<uidLength; i++) {
      Serial.print(uid[i], HEX);
    }
    Serial.print("\n");
    /*
    // Try to read the first general-purpose user page (#4)
    Serial.println("Reading page 4");
    uint8_t data[32];
    success = nfc.mifareultralight_ReadPage(4, data);
    if (success)
    {
      // Data seems to have been read ... spit it out
      nfc.PrintHexChar(data, 4);
      Serial.println("");

      // Wait a bit before reading the card again
      delay(1000);
    }
    else
    {
      Serial.println("Ooops ... unable to read the requested page!?");
    }
    */
  }
}

