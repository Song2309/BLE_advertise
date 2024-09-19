/***************************************************************************//**
 * @file
 * @brief Core application logic.
 *******************************************************************************
 * # License
 * <b>Copyright 2020 Silicon Laboratories Inc. www.silabs.com</b>
 *******************************************************************************
 *
 * SPDX-License-Identifier: Zlib
 *
 * The licensor of this software is Silicon Laboratories Inc.
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 *
 ******************************************************************************/
#include "em_common.h"
#include "app_assert.h"
#include "sl_bluetooth.h"
#include "gatt_db.h"
#include "app.h"
#include "em_gpio.h"
#include "sl_sensor_rht.h"
#include "custom_adv.h"


#define READING_INTERVAL_MSEC 5000
#define APP_TIMER_EXT_SIGNAL  0x01
static sl_sleeptimer_timer_handle_t app_timer;

CustomAdv_t sData; // Our custom advertising data stored here

static uint32_t humidity = 0;
static uint32_t temperature = 0;

// The advertising set handle allocated from Bluetooth stack.
static uint8_t advertising_set_handle = 0xff;
static void app_timer_callback(sl_sleeptimer_timer_handle_t *timer, void *data);

/**************************************************************************//**
 * Application Init.
 *****************************************************************************/
SL_WEAK void app_init(void)
{
  /////////////////////////////////////////////////////////////////////////////
  // Put your additional application init code here!                         //
  sl_sensor_rht_init();
  // This is called once during start-up.                                    //
  /////////////////////////////////////////////////////////////////////////////
}

/**************************************************************************//**
 * Application Process Action.
 *****************************************************************************/
SL_WEAK void app_process_action(void)
{
  /////////////////////////////////////////////////////////////////////////////
  // Put your additional application code here!                              //
  // This is called infinitely.                                              //
  // Do not call blocking functions from here!                               //
  /////////////////////////////////////////////////////////////////////////////
}

/**************************************************************************//**
 * Bluetooth stack event handler.
 * This overrides the dummy weak implementation.
 *
 * @param[in] evt Event coming from the Bluetooth stack.
 *****************************************************************************/
void sl_bt_on_event(sl_bt_msg_t *evt)
{
  sl_status_t sc;

  switch (SL_BT_MSG_ID(evt->header)) {
    // -------------------------------
    // This event indicates the device has started and the radio is ready.
    // Do not call any stack command before receiving this boot event!
    case sl_bt_evt_system_boot_id:
      // Create an advertising set.
      sc = sl_bt_advertiser_create_set(&advertising_set_handle);
      app_assert_status(sc);

      // Generate data for advertising
      sc = sl_bt_legacy_advertiser_generate_data(advertising_set_handle,
                                                 sl_bt_advertiser_general_discoverable);
      app_assert_status(sc);

      // Set advertising interval to 100ms.
      sc = sl_bt_advertiser_set_timing(
          advertising_set_handle,
          8000, // min. adv. interval (milliseconds * 1.6)
          8000, // max. adv. interval (milliseconds * 1.6)
          0,   // adv. duration
          0);  // max. num. adv. events
      app_assert_status(sc);
      // Start advertising and enable connections.
      sc = sl_bt_legacy_advertiser_start(advertising_set_handle,
                                         sl_bt_legacy_advertiser_connectable);
      //sl_bt_advertiser_set_channel_map(advertising_set_handle, 7);
      /*sl_bt_advertiser_set_channel_map(advertising_set_handle, 1);
      sl_bt_system_set_tx_power(0,0,0,0);
      sl_bt_advertiser_set_configuration(advertising_set_handle, 0);*/
      fill_adv_packet(&sData, 0x06, 0x02FF, temperature/1000, humidity/1000, "SonTong");
      start_adv(&sData, advertising_set_handle);
      init_timer();
      app_assert_status(sc);
      break;

      // -------------------------------
      // This event indicates that a new connection was opened.
    case sl_bt_evt_connection_opened_id:
      break;

      // -------------------------------
      // This event indicates that a connection was closed.
    case sl_bt_evt_connection_closed_id:

      // Generate data for advertising
      sc = sl_bt_legacy_advertiser_generate_data(advertising_set_handle,
                                                 sl_bt_advertiser_general_discoverable);
      app_assert_status(sc);

      // Restart advertising after client has disconnected.
      sc = sl_bt_legacy_advertiser_start(advertising_set_handle,
                                         sl_bt_legacy_advertiser_connectable);
      app_assert_status(sc);
      break;

      ///////////////////////////////////////////////////////////////////////////
      // Add additional event handlers here as your application requires!      //
      ///////////////////////////////////////////////////////////////////////////
    case sl_bt_evt_system_external_signal_id:
      update_and_advertise();
      break;
      // -------------------------------
      // Default event handler.
    default:
      break;
  }
}
void init_timer(void) {
  sl_status_t sc = sl_sleeptimer_start_periodic_timer_ms(&app_timer, READING_INTERVAL_MSEC, app_timer_callback, NULL, 0, 0);
  app_assert_status(sc);
}
static void app_timer_callback(sl_sleeptimer_timer_handle_t *timer, void *data) {
  (void)timer;
  (void)data;

  sl_bt_external_signal(APP_TIMER_EXT_SIGNAL);
}

void update_and_advertise(void)
{
  sl_sensor_rht_get(&humidity, &temperature);
  if(humidity != 0 && temperature != 0)
    update_adv_data(&sData, advertising_set_handle, temperature/1000, humidity/1000);
}
