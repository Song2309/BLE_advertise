#include <string.h>
#include "custom_adv.h"

#define ENCRYPTION_KEY 0xAB
static uint8_t encrypt(uint8_t data) {
  return data ^ ENCRYPTION_KEY;
}

void fill_adv_packet(CustomAdv_t *pData, uint8_t flags, uint16_t companyID, uint8_t temperature, uint8_t humidity, char *name)
{
  int n;

  pData->len_flags = 0x02;
  pData->type_flags = 0x01;
  pData->val_flags = flags;

  pData->len_manuf = 5;
  pData->type_manuf = 0xFF;
  pData->company_LO = companyID & 0xFF;
  pData->company_HI = (companyID >> 8) & 0xFF;

  pData->temperature = encrypt(temperature);
  pData->humidity = encrypt(humidity);

  // Name length, excluding null terminator
  n = strlen(name);
  if (n > NAME_MAX_LENGTH) {
      // Incomplete name
      pData->type_name = 0x08;
  } else {
      pData->type_name = 0x09;
  }

  strncpy(pData->name, name, NAME_MAX_LENGTH);

  if (n > NAME_MAX_LENGTH) {
      n = NAME_MAX_LENGTH;
  }

  pData->len_name = 1 + n; // length of name element is the name string length + 1 for the AD type

  // Calculate total length of advertising data
  pData->data_size = 3 + (1 + pData->len_manuf) + (1 + pData->len_name);
}

void start_adv(CustomAdv_t *pData, uint8_t advertising_set_handle)
{
  sl_status_t sc;
  // Set custom advertising payload
  sc = sl_bt_legacy_advertiser_set_data(advertising_set_handle, 0, pData->data_size, (const uint8_t *)pData);
  app_assert(sc == SL_STATUS_OK,
             "[E: 0x%04x] Failed to set advertising data\n",
             (int)sc);

  // Start advertising using custom data
  sc = sl_bt_legacy_advertiser_start(advertising_set_handle, sl_bt_legacy_advertiser_connectable);
  app_assert(sc == SL_STATUS_OK,
             "[E: 0x%04x] Failed to start advertising\n",
             (int)sc);
}

void update_adv_data(CustomAdv_t *pData, uint8_t advertising_set_handle, uint8_t temperature, uint8_t humidity)
{
  sl_status_t sc;
  // Update the two variable fields in the custom advertising packet
  pData->temperature = encrypt(temperature);
  pData->humidity = encrypt(humidity);

  // Set custom advertising payload
  sc = sl_bt_legacy_advertiser_set_data(advertising_set_handle, 0, pData->data_size, (const uint8_t *)pData);
  app_assert(sc == SL_STATUS_OK,
             "[E: 0x%04x] Failed to set advertising data\n",
             (int)sc);
}
