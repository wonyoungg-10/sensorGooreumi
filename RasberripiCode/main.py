import os
import time
import threading
import subprocess
import random
import pygame
import firebase_admin
from firebase_admin import credentials, db, storage
from mfrc522 import SimpleMFRC522
from datetime import datetime
import RPi.GPIO as GPIO
from datetime import datetime, timedelta

# Firebase 초기화
cred = credentials.Certificate('sensorprogramming-28ca1-firebase-adminsdk-kasq0-239b5a595f.json')
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://sensorprogramming-28ca1-default-rtdb.firebaseio.com',
    'storageBucket': 'sensorprogramming-28ca1.firebasestorage.app'
})

bucket = storage.bucket()

# 1-wire device initialization
os.system('modprobe w1-gpio')
os.system('modprobe w1-therm')

# Sensor IDs (replace with your actual sensor IDs)
sensor_ids = {
    "temp1": "28-29d1d4463a8b",
    "temp2": "28-6df6d4460bed"
}

# Sensor file paths
base_dir = '/sys/bus/w1/devices/'
device_files = {sensor: f"{base_dir}{sensor_id}/w1_slave" for sensor, sensor_id in sensor_ids.items()}

# Temperature monitoring thresholds
TEMP_THRESHOLD = 26.5
UPLOAD_DELAY = 3

# Functions for temperature monitoring and saving to Firebase
def read_temp_raw(device_file):
    with open(device_file, 'r') as f:
        lines = f.readlines()
    return lines

def read_temp(device_file):
    lines = read_temp_raw(device_file)
    while lines[0].strip()[-3:] != 'YES':  # Wait for sensor response
        time.sleep(0.2)
        lines = read_temp_raw(device_file)
    equals_pos = lines[1].find('t=')  # Find the temperature value
    if equals_pos != -1:
        temp_string = lines[1][equals_pos+2:]  # Extract the temperature value
        temp_c = float(temp_string) / 1000.0  # Convert millidegrees to Celsius
        return temp_c

def save_to_firebase(sensor_id, temperature):
    ref = db.reference(f'user/actions/temp/{sensor_id}')  # Save data under /actions/temp/{sensor_id}
    ref.set({
        'temperature': temperature,
    })
    




def monitor_sensor(sensor_id, device_file):
    last_measure_time = None  # When temperature exceeded threshold
    uploaded_once = False  # To ensure one upload per cycle

    while True:
        temperature = read_temp(device_file)
        print(f"{sensor_id}: {temperature}°C")
        current_time = time.time()

        if temperature >= TEMP_THRESHOLD:
            if last_measure_time is None:
                last_measure_time = current_time
                print(f"{sensor_id}: Temperature exceeded {TEMP_THRESHOLD}°C, starting timer.")

            elif current_time - last_measure_time >= UPLOAD_DELAY and not uploaded_once:
                save_to_firebase(sensor_id, temperature)
                print(f"{sensor_id}: Uploaded temperature {temperature}°C to Firebase.")
                uploaded_once = True
                
                temp_file_name = f"temperature_{round(temperature*4)/4.0:.2f}.mp3"
                audio_folder = "/home/pi/gooreumi/audio_files"
                audio_file_path = os.path.join(audio_folder, temp_file_name)
                play_mp3(audio_file_path)
        else:
            if last_measure_time is not None:
                print(f"{sensor_id}: Temperature dropped below {TEMP_THRESHOLD}°C, resetting.")
            last_measure_time = None
            uploaded_once = False

        time.sleep(1)  # Adjust the interval as needed

# Firebase file download and MP3 play functions
def download_audio(file_name, local_file_path):
    if os.path.exists(local_file_path):
        os.remove(local_file_path)  # 기존 파일 삭제
    print("Downloading MP3 file from Firebase Storage...")
    blob = bucket.blob(file_name)
    blob.download_to_filename(local_file_path)
    print(f"Downloaded {file_name} to {local_file_path}")

def play_mp3(file_path):
    try:
        subprocess.run(["mpg321", "-a", "hw:1,0", file_path], check=True)
    except Exception as e:
        print(f"Error: {e}")





# RFID functions and actions
reader = SimpleMFRC522()
card_to_action = {
    "224056443345": "playing",
    "801612038613": "feeding"
}

def update_database(action):
    ref = db.reference('user/actions')
    current_value = ref.child(action).get()
    if current_value is None:
        print(f"Action '{action}' not found in the database.")
        return
    new_value = current_value + 1
    ref.child(action).set(new_value)
    print(f"Updated '{action}': {new_value}")

def play_random_audio(action):
    audio_folder = "/home/pi/gooreumi/audio_files"
    if action == "playing":
        audio_files = ["play01.mp3", "play02.mp3", "play03.mp3"]
    elif action == "feeding":
        audio_files = ["feed01.mp3", "feed02.mp3", "feed03.mp3"]
    else:
        print("Unknown action, no audio to play.")
        return

    selected_audio = random.choice(audio_files)
    audio_file_path = os.path.join(audio_folder, selected_audio)
    print(f"Playing: {audio_file_path}")

    play_mp3(audio_file_path)

def read_rfid_and_execute():
    try:
        print("Place your card on the RFID reader...")
        while True:
            card_id, text = reader.read()
            card_id_str = str(card_id).strip()
            print(f"Detected Card ID: {card_id_str}")

            if card_id_str in card_to_action:
                action = card_to_action[card_id_str]
                update_database(action)
                play_random_audio(action)
            else:
                print("Card ID not recognized.")
    except KeyboardInterrupt:
        print("Program stopped by user.")
    finally:
        GPIO.cleanup()

# Checking time from Firebase and executing actions
def get_sleep_and_wake_time():
    ref = db.reference('user/userId')
    user_data = ref.get()
    sleep_time = user_data.get('sleepTime')
    wake_time = user_data.get('wakeTime')
    return sleep_time, wake_time

def is_time_to_execute(target_time):
    current_time = datetime.now().strftime("%H:%M")
    return current_time == target_time[:5]



def check_and_execute():
    while True:
        sleep_time, wake_time = get_sleep_and_wake_time()
        if is_time_to_execute(sleep_time):
            print(f"Time to sleep: {sleep_time}")
            download_audio("emotion_audio5.mp3", "/home/pi/gooreumi/audio_files/sleep.mp3")
            play_mp3("/home/pi/gooreumi/audio_files/sleep.mp3")
        elif is_time_to_execute(wake_time):
            print(f"Time to wake up: {wake_time}")
            download_audio("weather_audio.mp3", "/home/pi/gooreumi/audio_files/wakeup.mp3")
            play_mp3("/home/pi/gooreumi/audio_files/wakeup.mp3")
        time.sleep(30)

# Starting threads for each function
threads = []

# Monitor temperature sensors in parallel
for sensor_id, device_file in device_files.items():
    thread = threading.Thread(target=monitor_sensor, args=(sensor_id, device_file))
    thread.daemon = True
    threads.append(thread)
    thread.start()



# Read RFID tags and execute actions in parallel
thread_rfid = threading.Thread(target=read_rfid_and_execute)
thread_rfid.daemon = True
threads.append(thread_rfid)
thread_rfid.start()

# Check time-based actions in parallel
thread_time_check = threading.Thread(target=check_and_execute)
thread_time_check.daemon = True
threads.append(thread_time_check)
thread_time_check.start()

# Wait for all threads to finish (main thread)
try:
    while True:
        time.sleep(10)  # Keep the main program running
except KeyboardInterrupt:
    print("Program interrupted, exiting...")
