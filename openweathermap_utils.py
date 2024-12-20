import requests

def get_daily_average_temperature(city, api_key):
    """
    OpenWeatherMap API를 사용하여 하루 평균 기온 값을 가져옵니다.
    :param city: 도시 이름 (예: 'Seoul')
    :param api_key: OpenWeatherMap API 키
    :return: 하루 평균 기온 (int)
    """
    try:
        # OpenWeatherMap API URL 설정
        url = f"http://api.openweathermap.org/data/2.5/weather?q={city}&units=metric&appid={api_key}"
        
        # API 요청
        response = requests.get(url)
        response.raise_for_status()  # HTTP 오류 처리

        # JSON 데이터 파싱
        weather_data = response.json()
        temperature = weather_data["main"]["temp"]  # 현재 기온

        print(f"{city}의 현재 기온: {temperature}°C")
        return int(temperature)  # 정수 형태로 반환

    except Exception as e:
        print(f"기온 데이터를 가져오는 중 오류 발생: {e}")
        return None
