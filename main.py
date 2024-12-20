from utils.firebase_utils import (
    initialize_firebase,
    get_realtime_data,
    add_realtime_listener,
    get_storage_bucket,
    upload_audio
)
from utils.openai_utils import generate_response_with_openai
from utils.tts_utils import text_to_speech
from utils.openweathermap_utils import get_daily_average_temperature
import os
from datetime import datetime, timedelta


# 전역 변수로 사용자 이름과 Firebase 관련 설정 저장
name = None
output_audio_path = None
output_audio_path2 = None
last_weather_update = None
bucket = None


def process_temperature(temp1, temp2):
    """
    온도 데이터 - OpenAI API 호출/결과 처리.
    """
    global name, output_audio_path, bucket


    if temp1 is None and temp2 is None:
        print("온도 데이터가 없습니다.")
        return

    # 평균 온도 계산
    if temp1 is None:
        temperature = temp2
    elif temp2 is None:
        temperature = temp1
    else:
        temperature = (temp1 + temp2) / 2

    print(f"계산된 체온: {temperature}")

    # OpenAI GPT 프롬프트 생성
    openai_prompt = f"""
    너는 이름이 {name}인 사람이 키우는 작고 귀여운 강아지야.
    {temperature}은 너의 손을 잡은 사람의 손 온도야.
    26.5도가 손이 제일 차가운거고, 27.5도면 꽤 따뜻한거고, 그 29도 이상이면 엄청 뜨거운거야.
    항상 반말로만 말하고, 전체 응답은 짧게 20자 이내로 대답해줘.
    손 온도에 적절한 멘트를 출력해줘.
    ---
    예시
    체온: 36.8
    이름: 규리
    멘트: 규리야! 너 손 따뜻해서 좋다~

    체온: 36.2
    이름: 규리
    멘트: 규리야! 손이 좀 차갑다.

    체온: 36.5
    이름: 원영 
    멘트: 원영아! 내 손 부드럽지.
    ---
    체온: {temperature}
    이름: {name}
    멘트:"""

    response_text = generate_response_with_openai(openai_prompt)
    if not response_text:
        print("OpenAI API로부터 응답을 생성하지 못했습니다.")
        return

    print(f"OpenAI 응답: {response_text}")

    # TTS 변환 및 Firebase 업로드
    audio_file = text_to_speech(response_text, output_file=output_audio_path)
    print(f"음성 파일 생성 완료: {audio_file}")

    audio_url = upload_audio(bucket, audio_file)
    print(f"음성 파일이 Firebase Storage에 업로드되었습니다: {audio_url}")


def process_emotion(emotion):
    """
    emotion 데이터를 사용해 OpenAI API 호출 및 결과 처리.
    """
    global name, output_audio_path2, bucket

    if emotion is None:
        print("emotion 데이터가 없습니다.")
        return


    # OpenAI GPT 프롬프트 생성
    openai_prompt2 = f"""
    너는 이름이 {name}인 사람이 키우는 작고 귀여운 강아지야.
    {emotion}은 너를 키우는 사람의 오늘 하루 기분이야.
    항상 반말로만 말하고, 전체 응답은 70자 이내로 대답해줘.
    자기 전에 이 사람에게 해주고싶은 말을 해줘.
    ---
    예시
    규리야, 이제 잘 시간이야. 오늘 많이 힘들었구나. 조금만 버티면 놀 수 있으니까, 파이팅! 잘자!
    규리야, 좋은 밤이야. 늦게까지 놀고싶겠지만, 내일도 놀 수 있으니까 이제 그만 자자. 잘자!
    원영아, 이제 잘 시간이야. 내일 발표는 잘 할 수 있을거야. 굿나잇~
    """

    response_text = generate_response_with_openai(openai_prompt2)
    if not response_text:
        print("OpenAI API로부터 응답을 생성하지 못했습니다.")
        return

    print(f"OpenAI 응답: {response_text}")

    # TTS 변환 및 Firebase 업로드
    audio_file = text_to_speech(response_text, output_file=output_audio_path2)
    print(f"emotion 음성 파일 생성 완료: {audio_file}")

    audio_url = upload_audio(bucket, audio_file, file_name="emotion_audio5.mp3")
    print(f"emotion 음성 파일이 Firebase Storage에 업로드되었습니다: {audio_url}")


def process_weather(weather):
    """
    weather 데이터를 사용해 OpenAI API 호출 및 결과 처리.
    """
    global name, output_audio_path3, bucket

    if weather is None:
        print("weather 데이터가 없습니다.")
        return

    print(f"오늘의 날씨: {weather}°C")

    # OpenAI GPT 프롬프트 생성
    openai_prompt3 = f"""
    너는 이름이 {name}인 사람이 키우는 작고 귀여운 강아지야.
    {weather}은 오늘의 기온이이야.
    항상 반말로만 말하고, 전체 응답은 70자 이내로 대답해줘.
    이 사람을 잠에서 깨울 때 해줄 말을 출력해줘. 항상 기온을 먼저 언급해줘.
    예시
    규리야, 좋은 아침! 오늘 기온은 4도야. 따뜻하게 입고 나가. 오늘도 파이팅!
    규리야, 굿모닝! 이제 일어나야지. 오늘 기온은 영하 3도야. 진짜 추워. 코트 입지말고 패딩입어! 오늘도 파이팅!
    원영아, 이제 일어날 시간이야! 오늘 기온은 30도야. 엄청 더워! 시원한 물을 자주 마시면 좋겠다! 오늘도 파이팅!
    """

    response_text = generate_response_with_openai(openai_prompt3)
    if not response_text:
        print("OpenAI API로부터 응답을 생성하지 못했습니다.")
        return

    print(f"OpenAI 응답: {response_text}")

    # TTS 변환 및 Firebase 업로드
    audio_file = text_to_speech(response_text, output_file=output_audio_path3)
    print(f"weather 음성 파일 생성 완료: {audio_file}")

    audio_url = upload_audio(bucket, audio_file, file_name="weather_audio3.mp3")
    print(f"weather 음성 파일이 Firebase Storage에 업로드되었습니다: {audio_url}")


def update_weather_if_needed(api_key):
    """
    하루에 한 번만 날씨 데이터를 업데이트합니다.
    """
    global last_weather_update

    city = "Seoul"  # 서울로 고정

    # 하루에 한 번만 업데이트
    if not last_weather_update or datetime.now() - last_weather_update >= timedelta(days=1):
        print("하루 평균 날씨 데이터를 가져오는 중...")
        weather = get_daily_average_temperature(city, api_key)
        if weather is not None:
            process_weather(weather)
            last_weather_update = datetime.now()
        else:
            print("날씨 데이터를 가져오지 못했습니다.")
    else:
        print("오늘의 날씨는 이미 업데이트 되었습니다.")




def on_temp1_change(event):
    """
    temp1 경로 데이터 변경 이벤트를 처리하는 콜백 함수.
    """
    temp1 = event.data
    print(f"temp1 변경: {temp1}")
    process_temperature(temp1, temp2=None)  # temp2는 None으로 처리

def on_temp2_change(event):
    """
    temp2 경로 데이터 변경 이벤트를 처리하는 콜백 함수.
    """
    temp2 = event.data
    print(f"temp2 변경: {temp2}")
    process_temperature(temp1=None, temp2=temp2)  # temp1은 None으로 처리

def on_emotion_change(event):
    emotion = event.data
    print(f"emotion 변경: {emotion}")
    process_emotion(emotion)

def main():
    global name, output_audio_path, output_audio_path2, output_audio_path3, bucket

    # 프로젝트 경로 설정
    project_dir = "D:/ai_pet_project"
    utils_dir = os.path.join(project_dir, "utils")
    output_dir = os.path.join(project_dir, "output")

    # 파일 경로 정의
    service_account_path = os.path.join(utils_dir, "sensorprogramming-JSON -KEY")
    database_url = "https://sensorprogramming.com-KEY"
    output_audio_path = os.path.join(output_dir, "output_audio.mp3")
    output_audio_path2 = os.path.join(output_dir, "output_audio_2.mp3")
    output_audio_path3 = os.path.join(output_dir, "output_audio_3.mp3")

    # Firebase 초기화
    storage_bucket_name = "sensorprogramming-28ca1.firebasestorage.app"
    initialize_firebase(service_account_path, database_url, storage_bucket_name)
    bucket = get_storage_bucket()

    # 사용자 이름 가져오기 (한 번만 실행)
    name_path = "user/userId/name"
    name = get_realtime_data(name_path)

    if name is None:
        print("사용자 이름 데이터를 가져올 수 없습니다.")
        return

    print(f"사용자 이름: {name}")

    # 하루 평균 날씨 업데이트
    OPENWEATHER_API_KEY = "API-KEY"  # OpenWeatherMap API 키
    update_weather_if_needed(OPENWEATHER_API_KEY)

    # Realtime Database 리스너 설정
    temp1_path = "user/actions/temp/temp1/temperature"
    temp2_path = "user/actions/temp/temp2/temperature"
    emotion_path = "entries/emotion"

    add_realtime_listener(temp1_path, on_temp1_change)
    add_realtime_listener(temp2_path, on_temp2_change)
    add_realtime_listener(emotion_path, on_emotion_change)


if __name__ == "__main__":
    main()
