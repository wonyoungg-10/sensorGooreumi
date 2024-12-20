import openai

# OpenAI API를 사용하여 응답 생성
def generate_response_with_openai(prompt):
    openai.api_key = "OPENAI-KEY"  # 사용자의 OpenAI API 키 입력

    try:
        response = openai.ChatCompletion.create(
            model="gpt-3.5-turbo",  # 최신 모델로 변경
            messages=[
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": prompt},
            ],
            max_tokens=100,
            temperature=0.7,
        )
        return response['choices'][0]['message']['content'].strip()
    except Exception as e:
        print(f"OpenAI API 호출 중 오류 발생: {e}")
        return None
