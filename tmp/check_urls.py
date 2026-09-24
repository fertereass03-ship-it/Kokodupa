import re
import urllib.request
from concurrent.futures import ThreadPoolExecutor

files = [
    "app/src/main/java/com/example/data/api/AniListService.kt",
    "app/src/main/java/com/example/data/repository/AnimeRepository.kt",
    "app/src/main/java/com/example/data/schedule/AnimeScheduleData.kt"
]

urls = set()
for path in files:
    with open(path) as f:
        text = f.read()
    found = re.findall(r"https?://[^\s\"\'\<\>]+", text)
    for u in found:
        if any(ext in u.lower() for ext in [".jpg", ".jpeg", ".png", ".webp"]):
            urls.add(u)

print(f"Total image URLs to test: {len(urls)}")

def check(u):
    req = urllib.request.Request(u, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
    try:
        with urllib.request.urlopen(req, timeout=4) as resp:
            return (u, resp.status == 200, resp.status)
    except Exception as e:
        return (u, False, str(e))

with ThreadPoolExecutor(max_workers=20) as ex:
    results = list(ex.map(check, urls))

broken = [r for r in results if not r[1]]
print(f"Tested {len(results)}, broken count: {len(broken)}")
for b in broken:
    print("BROKEN:", b[0], "-->", b[2])
