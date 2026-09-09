import re
import sys

try:
    text = open('donor/app.html', encoding='utf-8').read()
    babel = re.search(r'<script[^>]*type=[\'\"]text/babel[\'\"][^>]*>', text)
    if not babel:
        print('No babel script found')
    else:
        script = text[babel.end():text.find('</script>', babel.end())]
        b = 0; p = 0; sq = False; dq = False; bq = False; escape = False
        for i, c in enumerate(script):
            if escape: 
                escape = False
                continue
            if c == '\\\\': 
                escape = True
                continue
            if sq or dq or bq:
                if c == '\'' and sq: sq = False
                elif c == '\"' and dq: dq = False
                elif c == '`' and bq: bq = False
                continue
            if c == '\'': sq = True
            elif c == '\"': dq = True
            elif c == '`': bq = True
            elif c == '{': b += 1
            elif c == '}': b -= 1
            elif c == '(': p += 1
            elif c == ')': p -= 1
            
            if b < 0:
                print(f'Negative brace at {i}: {script[max(0, i-50):i+50]}')
                break
            if p < 0:
                print(f'Negative parens at {i}: {script[max(0, i-50):i+50]}')
                break
        print(f'Final braces: {b}, parens: {p}')
except Exception as e:
    print(f'Error: {e}')
