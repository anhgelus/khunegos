import {defineConfig} from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
    title: "Khunegos",
    description: "Wiki of Khunegos",
    themeConfig: {
        // https://vitepress.dev/reference/default-theme-config
        nav: [
            {text: 'Home', link: '/'},
            {text: 'Examples', link: '/markdown-examples'}
        ],

        sidebar: [
            {
                text: 'Examples',
                items: [
                    {text: 'Markdown Examples', link: '/markdown-examples'},
                    {text: 'Runtime API Examples', link: '/api-examples'}
                ]
            }
        ],

        socialLinks: [
            {icon: 'github', link: 'https://github.com/anhgelus/khunegos'}
        ],

        footer: {
            message: 'Released under the AGPL License. <a href="https://www.anhgelus.world/legal/" target="_blank">Legal information</a>.',
            copyright: 'Copyright © 2025 William Hergès'
        }
    }
})
