-- command 为自动化测试内置的库. 目前包含 iozone ,pull 命令, 后续会支持shell 命令
-- log 为内置的日志输出命令,会将日志输出至文件及控制台.  log 有两个参数,第一个为tag, 第二个为msg. tag是为了后续进行筛选使用的.
-- version: 0.0.1
require 'command'

log("iozone", "Start iozone test!")

testItemPercent = {70, 15, 4, 2, 1, 1, 2, 1, 4}
testItemBs  = {4, 8,  12 , 16,  20 , 32,  36 , 40 , 512}

availableCap = command.shell("df | grep data | grep -v media | tr -s ' ' | cut -d ' ' -f 4")
availableCap = availableCap/1024 - 400

emmcSize = command.shell("df | grep data | grep -v media | tr -s ' ' | cut -d ' ' -f 2")
emmcSize = emmcSize / 1024
totalPercent = 0
TBWSize = 0
currTestCap = 0
if 8192 > emmcSize then
    TBWSize = 8 * 1024 * 1024
elseif 16384 > emmcSize then
    TBWSize = 10 * 1024 * 1024
else
    TBWSize = 15 * 1024 * 1024
end


for i = 1, #testItemPercent do
    totalPercent = testItemPercent[i] * testItemBs[i] + totalPercent
    print(totalPercent)
    print(testItemBs[i])
end

log('iozone', string.format('TBWSize:%d!',TBWSize))
log('iozone', string.format('emmcSize:%d!',emmcSize))
log('iozone', string.format('availableCap:%d!',availableCap))

local earlyBreak = false
while true do

    command.shell("rm /mnt/sdcard/*.io")
    availableCap = command.shell("df | grep data | grep -v media | tr -s ' ' | cut -d ' ' -f 4")
    availableCap = availableCap/1024 - 400

    for i=1,#testItemPercent do
        log('iozone', string.format('start No.%d test!',i))
        local itemCap = availableCap * testItemPercent[i] * testItemBs[i] / totalPercent
        local options = string.format('-w -i0 -i2 -r%dk -s%dm -f /mnt/sdcard/%d_seq.io',testItemBs[i], itemCap ,testItemBs[i])
        log('iozone', options)
        log('iozone', command.iozone(options))
        ----    command.pull("/mnt/sdcard/test.bin", "/Users/v1ki/IdeaProjects/automate_pc/results")
        log('iozone', string.format('No.%d test completed!',i))
        currTestCap = currTestCap + itemCap * 3
        if currTestCap >= TBWSize then
            earlyBreak = true
            break
        end
    end
    log("iozone", "iozone test completed!")
    if earlyBreak then
        break
    end
end

log("iozone", "iozone test completed!")